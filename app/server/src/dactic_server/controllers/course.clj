(ns dactic-server.controllers.course
  (:require
            [liberator.core :refer [defresource]]
            ;;validation
            [noir.validation :as validation :refer [rule errors? has-value? get-errors on-error is-email?]]
            ;;utils
            [dactic-server.controllers.util :as utils]
            ;; users
            ;;controllers shouldnt call each other...
            [dactic-server.controllers.auth :as auth]
            [dactic-server.models.auth :as auth-utils]
            ;;db
            [dactic-server.models.course :as course-db]
            [dactic-server.models.util :as db-util]
            [dactic-server.models.auth :as user-util]
            
            ;;string slugification
            [slugger.core :refer [->slug]]
            ;;session
            [noir.session :as session]
            
            ))

;;;;;;;;; Helper funcs ;;;;;;;;;

(defn generate-nice-dates
  "expects seq of java date"
  [dates]
  (map #(format "%1$ta %1$te %1$tb" %) dates)
  )

(defn generate-detail
  [entity]
  (let [attrs (db-util/get-full-attrs entity)]
    (when-not (nil? attrs)
      (merge
       attrs
       {:eid entity}
       {:about-instructor (or (db-util/get-instructor-about entity) "This instructor hasn't told us much yet.")}))))

(defn get-course-detail
  [e & [is-inst?]]
  (let [
        is-inst? (or is-inst? false)
        course (select-keys
                (generate-detail e)
                [:title :short-description :programming-environment :state :description :venue :header-image :eid :takeaways :plan :cost :slug :dates :available-spaces :instructor :instructor-about :resources ])]
    (assoc course
      :dates (generate-nice-dates (sort (:dates course)))
      :inst? is-inst?)))

;;;;;;;;;;;;;;;;;;

(defn reg-validation [{plan :plan short-desc :short-description takeaways :takeaways description :description title :title}]
  ;; (rule (session/get :user false)
  ;;       [:course-plan "login is required"])
  (rule (has-value? title)
        [:course-title "Course title is required"])
  (rule (has-value? short-desc)
        [:course-short-description "Short description is required"])
  (rule (has-value? description)
        [:course-description "Description is required"])
  (rule (#(and ( has-value? %) (not (empty? %))) plan)
        [:course-plan "Course plan is required"])
  
  
  (rule (#(and ( has-value? %) (not (empty? %))) takeaways)
        [:course-takeaways "Course takeaways is required"])
  

  (utils/create-errors-vec [:course-plan :course-short-description :course-description :course-title :course-takeaways]))





(defn prepare-for-save-course
  "manipulates the data into a DB friendly form"
  [{:keys [state plan slug short-description description takeaways instructor-about title programming-environment instructor]
    :or {plan ""
         short-description ""
         description ""
         takeaways ""
         programming-environment ""
         instructor-about ""} :as course}]
  (let [state (case state
                "inProgress" :course.state/pre-submission
                "submit" :course.state/unapproved)]
    ;; convert array  in string representation. datomic databasee doesnt have
    ;; an ordered list type :( 
    {:course/plan (pr-str plan)
     :course/slug (->slug title)
     :course/short-description short-description
     :course/takeaways (pr-str takeaways)
     :course/instructor instructor
     :course/description description
     :course/title title
     :course/state state
     :course/programming-environment (str programming-environment)
     :course/instructor-about instructor-about}))

(defresource course-resource
  [{:keys [state title cid] :or {cid nil} :as args}]
  :available-media-types ["application/json"]
  :allowed-methods [:post :put]
  :can-put-to-missing? true
  :malformed? (fn [_]
                (if (= state "submit")
                  (let [valid (reg-validation args)]
                    (if (= true valid)
                      false
                      (assoc valid 0 true)))
                  false))
  :allowed? (fn [ctx]
              ;;first and foremost, he must be logged in to do any
              ;;course stuff
              (if-let [user-id (session/get :user-id false)]
                
                (let [course (prepare-for-save-course (assoc args :instructor user-id))]
                  
                  (.println System/out (str "doing something to " course))
                  (try
                    @(db-util/transact [[:upsert-course course  (->slug title) user-id]] true)
                    (catch Exception e
                      (.println System/out e)
                      [false {:errors {:auth ["Unable to save course to db"]}
                              :representation {:media-type "application/json"}}] )))
                [false {:errors {:auth ["not logged in"]}
                        :representation {:media-type "application/json"}}]))
  :handle-forbidden utils/errors-in-ctx
  :exists? (fn [_] (course-db/course-exists? (->slug title)))
  ;;  :respond-with-entity? true
  :post! (fn [_] {::entry {:course-message (if (= state "submit")
                                            "Course submitted for approval, we'll be in touch"
                                            "Course progress saved!")}})
  :handle-ok ::entry
  :handle-created ::entry
  :handle-malformed utils/errors-in-ctx)

(defresource course-entry-resource [id & put-data]
  :allowed-methods [:get]
  :malformed? (fn [ctx]
                
                [false {::data (first put-data)}])
  :exists? (fn [_]
             ;;this shoud probably me handled in model - controllers
             ;;shouldnt call db util directly. Perhaps there should be
             ;;a list of attribute names to be looked up. and we need
             ;;to deserialize the list types
             ;;Also, get should return a map not an array.

             (let [e (->> (db-util/query (db-util/get-arb "course" [] {:slug id}))
                        (ffirst)) ]
               (when-not (nil? e)
                 {::entry (get-course-detail e (auth-utils/is-course-instructor-logged-in? {:slug id} (session/get :user-id)))})))
  :available-media-types ["application/json"]
  :can-put-to-missing? false
  :respond-with-entity? true
  :handle-ok ::entry)

;;;;;;;;; Course List Resource ;;;;;;;;;;;;

(defn get-course-list
  [logged-in?]
  ;;If he's logged in, give him the extras
  (let [extras (if logged-in? [:resources] [])]
    (map (fn [e] (get-course-detail (first e))) (db-util/query (db-util/get-arb "course" [] {:state :course.state/live})))
    ))
;;
(defresource course-list-resource
  []
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :handle-ok (fn [_]
               
               (get-course-list (session/get :user false))))


(defn handle-course-links-save
  [ {url :url desc :description course :cid }]
  (course-db/save-and-associate-link url desc course))

(defn handle-course-links-delete
  [{eid :eid }]
  
  (db-util/save-arb (db-util/retract-entity-tx (read-string  eid))))

(defresource course-links-resource
  [{:as args}]
  :available-media-types ["application/json"]
  :allowed-methods [:post :delete]
  :allowed? (fn [_] (if ( auth-utils/is-course-instructor-logged-in? (args :title) (read-string (str (args :cid))))
                     true
                     [false {:errors ["not allowed"]
                             :representation {:media-type "application/json"}}]))
  :handle-forbidden utils/errors-in-ctx
  :post! (fn [_] (handle-course-links-save args))
  :delete! (fn [_] (handle-course-links-delete args))
  )



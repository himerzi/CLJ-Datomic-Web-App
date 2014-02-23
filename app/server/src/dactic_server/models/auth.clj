(ns dactic-server.models.auth
  (:require
            ;;sessions
            [noir.session :as session]
            [ring.middleware.session.memory :refer [memory-store]]
            ;;md5 hashing, for building gravatar stuff
            [digest :as digest]
            ;;DB
            [dactic-server.models.util :as db-util]
            ;;decorator helpers
            [dactic-server.decorate :as decorate]
            ))
;;;;;;;;; helper ;;;;;;;;;

(defn to-gravatar-str
  [email]
  (str "http://www.gravatar.com/avatar/" (digest/md5 email)))
(defn email-unique?
  [email]
  (empty? (db-util/query (db-util/get-arb :user [] {:email email}))))

(defn save-user
  [user]
  (if (not (empty? (db-util/query (db-util/get-arb :user [] {:email (user :email)}))))
    [false "email allready exists"]
    @(db-util/save-arb (db-util/generate-save-transaction  "user" (merge {} user)) true)))

(defn get-eid-from-tx
  "A decorator, used to pull an entity id out of a transaction return value"
  [func]
  (fn [& args]
    (let [results (apply func args)]
      {:id (:e (last (:tx-data results)))})))

(decorate/decorate save-user get-eid-from-tx)

(defn is-course-instructor-logged-in?
  [{ :as course-identifier} uid & [slug]]
  "can find course by title or slug"
  (let [uid (or uid 0)
        search-term (if (contains? course-identifier :slug)
                      {:slug (course-identifier :slug)}
                      {:title (course-identifier :title)})
        course  (first (db-util/query (db-util/get-arb "course" [:instructor] search-term)))
        course-inst (first course)]
    (if (= course-inst uid )
      true
      false))
  )


(defn get-arb-detail
  [eid keys & [extras]]
  (let [
        attrs (db-util/get-full-attrs eid)]
    (when-not (nil? attrs)
      (->
       (select-keys attrs keys)
       (merge extras)))))

;;zee towah of powah

(def get-user-detail-q
  (db-util/q-builder '[?f-name ?l-name ?email ?title ?slug ?state ]
                     '[[?e :user/first-name ?f-name]
                       [?e :user/email ?email]
                       [?e :user/last-name ?l-name]
                       [?c :course/instructor ?e]
                       [?c :course/title ?title]
                       [?c :course/slug ?slug]
                       [?c :course/state ?st]
                       [?st _ ?state]]
                     '[?e]))

(defn get-user-detail
  [eid own-profile?]
  (let [
        extras {:id eid}
        result (db-util/query get-user-detail-q eid)
        name (ffirst result)
        hash (digest/md5  (nth (first result) 2))
        last-name (second (first result))
        ;;drop 3, because we don't want the name and email information
        ;;in courses info
        courses (vec (map (fn [a] (zipmap [:title :slug :state] (drop 3 a))) result))
        owner-extras (if own-profile?
                       {:inProgress (filter #(= (:state %) :course.state/pre-submission) courses)
                        :unapproved (filter #(= (:state %) :course.state/unapproved) courses)}
                       {})
        ]
    (.println System/out (str "****" courses))
    (into {} (assoc extras
               :first-name name
               :last-name last-name
               :gravatar (str "http://www.gravatar.com/avatar/" hash)
               :courses (merge owner-extras
                               {:live (filter #(= (:state %) :course.state/live) courses)}))))) 






(ns dactic-server.controllers.auth
  (:require
            [liberator.core :refer [defresource]]
            ;;validation
            [noir.validation :as validation :refer [rule errors? has-value? get-errors on-error is-email?]]
            ;;utils
            [dactic-server.controllers.util :as utils]
            ;;db
            [dactic-server.models.util :as db-util]
            [dactic-server.models.auth :as auth-db]
            ;;crypt
            [noir.util.crypt :as crypt]
            ;;session
            [noir.session :as session]
            ;;string slugification
            [slugger.core :refer [->slug]]
            ;;DB
            [dactic-server.models.util :as db-util]
            ))
;;;;;;;;; Helper fns ;;;;;;;;;
(defn set-login-session
  [email id]
  (session/put! :user email)
  (session/put! :user-id id)
  )
(defn reg-validation [{name :name pass :password email :email}]

  (rule (has-value? name)
        [:name "name is required"])
  (rule (<= 2 (count (clojure.string/split (clojure.string/triml name) #"\s+" )))
        [:name "A first name and last name are required"])
  (rule (is-email? email)
        [:email "email invalid"])
  (rule (auth-db/email-unique? email)
        [:email "email is already registered"])
  (rule (has-value? email)
        [:email "email is required"])
  (rule (has-value? pass)
        [:password "password is required"])
  (rule (validation/min-length? pass 4)
        [:password "password must be longer than 3 characters"])
  (if (errors? :name :password :email)
    ;;The booleans answer the question "allowed?"
    [false {:errors
            (->> (filter #(not (= nil (get-errors %))) [:name :password :email])
                 (mapcat (fn [x] [x (get-errors x)]))
                 (apply array-map ))
           :representation {:media-type "application/json"}
             }]
    true))

(defn handle-user-save
  ;;all saves should go through here
  [user-details {:as opts }]
  (let [split-name (clojure.string/split (clojure.string/triml (or  (:name user-details) "")) #"\s+" )
        first-name (first split-name)
        last-name (last split-name)
        save (auth-db/save-user (-> user-details
                           (dissoc :name)
                           (assoc :first-name first-name :last-name last-name)
                           (merge opts)
                           (assoc :password (crypt/encrypt (user-details :password)))))]
    (session/clear!)
    (set-login-session (user-details :email) (:id save))
    save))
(defresource registration-resource
  [name email pass]
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :allowed? (fn [ctx] (reg-validation {:name name :email email :password pass}))
  :handle-forbidden utils/errors-in-ctx
  :handle-created (fn [ctx] {:id (ctx :id)})
  :post! (fn [_] ( handle-user-save {:name name :email email :password pass } {:type :user.type/user}))
  )

;;;;;;  Login Logic ;;;;;;

(defn handle-login [id pass]

  (rule (has-value? id)
        [:id "screen name is required"])
  (rule (has-value? pass)
        [:pass "password is required"])
  (let [user (db-util/query (db-util/get-arb "user" [:email :password] {:email id}))]
    (rule (and (not (empty? user)) (crypt/compare pass (second (first user)))) [:pass "invalid email/password"])
    (utils/create-errors-vec [:id :pass] (fn []
                                           (session/put! :user id)
                                           (session/put! :user-id (last (first user)))
                                           (.println System/out (str "user is " id " session says " (session/get :user id)))
                                           [true {:user-id (last (first user))}]))))


(defresource login-resource
  [email pass]
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :allowed? (fn [ctx]
              (let [email (or email "")
                    pass (or pass "")]  (handle-login email pass)))
;;  :post! (fn [ctx] {::location "http://example.com"})
;;  :post-redirect? false
  :handle-created (fn [{l :user-id }] {:profile-location l })
  :handle-forbidden utils/errors-in-ctx
  )

(defresource logout-resource
  []
  :available-media-types ["application/json"]
  :allowed-methods [:delete]
  :allowed? true
  :delete! (fn [_] (session/remove! :user-id) (session/remove! :user))
  )

;;;;;;;;; User Resource ;;;;;;;;;

(defresource user-resource
  []
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok  (fn [_] (if (session/get :user-id false) {:loggedIn true :id (session/get :user) :profile (session/get :user-id)} {:loggedIn false})))


(def krisxtra
  {:past [{:title "Introduction to Clojure"}]
   :testimonials ["I had a very positive experience with the Clojure Crash Course. Taught by professionals, the course content was very focused and practical. My confidence with Clojure increased significantly over the 4 sessions. I also enjoyed the instructor's relaxed and humour filled delivery. In summary, I would recommend this experience and look forward to taking more courses with Dactic in future. - Paddy Gallagher", "I've learned many things from the course that I'd struggled with for a long time when using books or tutorials on the internet - there is no replacement for hands on training. The teachers have a huge amount of experience and seem enthused to give us a glimpse of how powerful Clojure can be and were able to go in to great depth when asked questions about the language. I was surprised by how fast the group was able to build up to working with complex data from Spotify or weather forecasts. My final thoughts are that I want to use Clojure for everything! - Luke Snape", ]})

(defresource user-detail-resource
  [id]
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok  (fn [_] (let [return ( auth-db/get-user-detail (read-string id) (= (read-string id) (session/get :user-id)))]
                       return)))

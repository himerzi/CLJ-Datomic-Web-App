(ns dactic-server.models.util
  (:require
   [datomic.api :as d :refer [db q]]
   [digest :as digest]
   [dactic-server.decorate :as decorate]))

(def conn (atom nil))

;;This avoids trying to @connect to a database during AOT compilation
(defn -init
  []
  (let [uri (format "datomic:ddb://eu-west-1/datomicddb/%s?aws_access_key_id=%s&aws_secret_key=%s"
                    (if (System/getenv "PROD") "production" "dactic-test" )
                    (System/getenv "AWS_ACCESS_KEY_ID")
                    (System/getenv "AWS_SECRET_KEY"))]
    (reset! conn (d/connect uri))))

;;;;doesn't really belong here
(defn to-gravatar-str
  [email]
  (str "https://www.gravatar.com/avatar/" (digest/md5 email)))
;;;;;;;;;;;






;; little helper fns

(defn remove-key-ns
  "removes namespace from keys in a map"
  [entry]
  (letfn [(strip-ns
            [key]
            (keyword (second (clojure.string/split (str key) #"/"))))
          (prep-entry [entry]
            (clojure.set/rename-keys entry (zipmap (keys entry) (map strip-ns (keys entry)))))]
    (prep-entry entry)))

;;this should replace query-arb. Simpler. better.
(defn q-builder
  [[:as find] [:as where] & [[:as in]]]
  {:find find :where where :in (cons '$ in)})


;;Behold, the beauty of FP + Datomic
(defn query-arb
  "Query abritrary attrs (the arguments) should be keys"
  [attrs & [{:as attr-vals :or false}]]
  (let [
        valnames (map #(symbol (str "?" (name %))) attrs)
        attr-vals  (when attr-vals (apply map list attr-vals))
        x-terms (if attr-vals
                  (map #(vector '?id %1 %2) (concat attrs (first attr-vals))
                       (concat valnames (second attr-vals)))
                  (map #(vector '?id %1 %2) attrs valnames))
        query {:find (concat valnames ['?id])
               :where x-terms}]
    query))


(defn add-ns
  "returns list of keyword of the form :ns-name/attrs"
  [ns-name attrs]
  (map #(if-not (namespace %) (keyword (name ns-name) (name %)) %) attrs))

(defn map-add-ns
 "adds namespace as specified in ident to every key in supplied map"
  [ident {:as attr-values}]
  (clojure.set/rename-keys attr-values (zipmap (keys attr-values) (add-ns ident (keys attr-values)))))

(defn get-arb
  "Get Arbitrary. at minimum must supply entity namespace, and one identifying attribute, attr values is a map specifying which attribute values you want to query for"
  [ident attrs & [{:as attr-values}]]

  (let [args (if attr-values
               [(add-ns ident attrs) (map-add-ns ident attr-values)]
               [(add-ns ident attrs)])]
    (apply query-arb args)))

(defn query
  [args & [data-src]]
  (q args (db @conn) (or data-src []))
  )


(defn generate-save-transaction
  [ident {:as attr-values} & [existing-id]]
  (conj {:db/id (or existing-id (d/tempid :db.part/user))} (map-add-ns ident attr-values)))

(defn save-arb
  "save arbitrary things. tx is the transaction"
  [transaction-map & [sync?]]
  (let [tx-fun (if (or sync? false) d/transact d/transact-async)]
    ;;need to save as a list bits as a list ref uh-oh
    (tx-fun @conn [transaction-map])))

(defn transact
  "save arbitrary things. tx is the transaction same as above but expects a list of things"
  [transaction-list & [sync?]]
  (let [tx-fun (if (or sync? false) d/transact d/transact-async)]
    ;;need to save as a list bits as a list ref uh-oh
    (tx-fun @conn transaction-list)))

(defn retract-entity-tx
  [eid]
  [:db.fn/retractEntity eid])

(defn get-full-attrs
  [eid]
  (merge {} (d/entity (db @conn) eid)))

(defn wrap-full-attrs
  [func]
  (fn [& args]
    (remove-key-ns (apply func args))))

(defn check-for-lists
  [collection]
  (if (and (contains? collection :plan) (contains? collection :takeaways))
    (-> (assoc collection :plan (read-string (collection :plan)))
        (assoc :takeaways (read-string (collection :takeaways))))
    collection) )

(defn parse-encoded-lists [func]
  (fn [& args]
    (check-for-lists (apply func args))))

(defn unwrap-course-resources
  "if this thing has a resources key, make sure theyre touched to their full values"
  [func]
  (fn [& args]
    (let [get-resource (fn [eid]
                         (select-keys (get-full-attrs eid) [:url :description]))
          results (apply func args)]
      (if (contains? results :resources)
        (assoc results :resources (map #(select-keys (d/touch %) [:resource/url :resource/description :db/id]) (results :resources)))
        results
        ))))

(defn unwrap-course-instructors
  "if this thing has a resources key, make sure theyre touched to their full values"
  [func]
  (fn [& args]
    (let [results (apply func args)]
      (if (contains? results :instructor)
        (let [inst (d/touch (first (results :instructor)))]
          (-> results
              (assoc
                  :instructor (select-keys inst [:user/first-name :user/last-name :db/id]))
              (assoc-in [:instructor :gravatar] (to-gravatar-str (:user/email inst)))))
        results
        ))))
(defn unwrap-course-venue
  "same as above, but for venue. wants to be refactored"
  [func]
  (fn [& args]
    (let [results (apply func args)]
      (if (contains? results :venue)
        (let [inst (d/touch (results :venue))]
          (-> results
              (assoc
                  :venue (select-keys inst [:venue/logo-img :venue/address :venue/name]))))
        results))))
(defn inject-missing-course-attrs
  "if this is a course, then inject course resources (ie links that the instructor posts)"
  [func]
  (fn [& args]
    (let [ results (apply func args)]
      ;;basically, checking if this ia a course
      (if (and (contains? results :course/slug) (not (contains? results :course/resources)))
        (assoc results :course/resources [])
        results
        ))))
(decorate/decorate get-full-attrs inject-missing-course-attrs  wrap-full-attrs parse-encoded-lists unwrap-course-instructors unwrap-course-venue  unwrap-course-resources)

(defn get-instructor-about
  "for a given course-id, returns the about information for an instructor"
  [course-id]
  (ffirst (q '[:find ?about
              :in $ ?course-id
              :where
              [?course-id :course/instructor ?i]
               [?i :user/about ?about]] (db @conn) course-id)))



 



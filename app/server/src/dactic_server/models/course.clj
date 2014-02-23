(ns dactic-server.models.course
  (:require
   [datomic.api :as d :refer [db q]]
   [dactic-server.models.util :as db-util]))

;;Helper fn

(defn course-exists?
  [slug]
  (let [e (ffirst (db-util/query (db-util/get-arb "course" [] {:slug slug}))) ]
    (if e
      [true e]
      false)))

;;need to enforce some kind of uniqueness so people dont spam me
;;courses
;;if a resource is in the save, make sure we delete all previous
;;resources.... so put works
(defn save-course
  "need to have instructor email"
  [{:as course} & [{:as opts}]]
  (db-util/save-arb (db-util/generate-save-transaction "course"
                                                       course) true))

;; def fields

(def fields [:title :short-description :programming-environment :state :description :venue :header-image :eid :takeaways :plan :cost :slug :dates :available-spaces :instructor :instructor-about :resources ])
;; def permissions
;;could take array form, as above, or specify permissions as well

(def permissions {:title [:public] :short-description [:public] :programming-environment [:public] :state [:public] :description [:public] :venue [:public] :header-image [:public] :eid [:public] :takeaways [:public] :plan [:public] :cost [:public] :slug [:public] :dates [:public] :available-spaces [:public] :instructor [:public] :instructor-about [:public] :resources [:logged-in]})

;;Not deprecated!
(defn save-and-associate-link
  [url desc course-eid]
  
  (db-util/transact [(assoc (db-util/generate-save-transaction "resource" {:url url :description desc})
                         :course/_resources course-eid)]))


(defn gen-list-of-dates
  "expects a list of maps [{:d :m}...] note months are converted to 0  indexed"
  [dates]
  (map #(java.util.Date. (.getTimeInMillis
                          ( doto (java.util.Calendar/getInstance (.  java.util.Locale UK))
                            (.set  (.  java.util.Calendar DAY_OF_MONTH) (:d %))
                            (.set (. java.util.Calendar MONTH) (dec (:m %))))))
       dates))




 



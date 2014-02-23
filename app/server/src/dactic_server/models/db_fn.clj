(ns dactic-server.models.db-fn
  (:require
   [datomic.api :as d :refer [db q]]
   [dactic-server.models.util :refer [conn]]
   ;;validation
   [noir.validation :as validation :refer [rule errors? has-value? get-errors on-error is-email?]]))

;; defines an DB function 
(def upsert-course
  "Defines a database function (itself an entity) that enforces constraints on upserting courses."
  [{:db/id #db/id [:db.part/user]
    :db/ident :upsert-course
    :db/fn (d/function
            '{:lang :clojure
              :params [db course slug instructor]
              :code (
                     let [inst-exists? (if (or (nil? instructor)  (not-empty (d/q '[:find  ?ins
                                                                                    :in $ ?ins
                                                                                    :where
                                                                                    [?ins :user/email _]]
                                                                                  db instructor)))
                                         true
                                         (throw (Exception. (str instructor " is a non existing instructor value"))))
                          course-qry (d/q '[:find (distinct ?instructors) ?e
                                            :in $ ?slug
                                            :where
                                            [?e :course/slug ?slug]
                                            [?e :course/instructor ?instructors]]
                                          db slug)
                          course-exists? (= 1 (count course-qry))
                          is-inst? (contains?  (ffirst course-qry) instructor)
                          temp-id (d/tempid :db.part/user)
                          course (assoc course :db/id temp-id)]
                      (cond
                       (and course-exists? (not is-inst?)) (throw (Exception. (str "Attempted to update course where " instructor " is not instructor.")))
                       
                       ;;if the course doesn't exist, and we have a
                       ;;valid instructor id, then this is a new course
                       :else [course] 
                       )
                      )})}])


(defn -init
  []
  (d/transact @conn upsert-course))



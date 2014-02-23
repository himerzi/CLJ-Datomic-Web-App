(ns dactic-server.models.fmk
  (:require
   [datomic.api :as d :refer [db q]]
   [dactic-server.models.util :as db-util]))


(defn- state->rule-set
  "given a mapping from fields to states in which they are permitted to be exposed, returns a map of which fields should and shouldn't be visible. e.g. returns {fieldname true|false}"
  [{:as permissions} {:as state}]
  )
(defn- perm-check
  "compares data against a map describing permitted fields, and removes stuff stuff from data that isn't permitted"
  [data permitted-fields]

  )
;;Thanks Pelle
(defn touch-everything
  "Simple function to pull out all the attributes of an entity into a map"
  [entity]
  (select-keys entity (keys entity)))

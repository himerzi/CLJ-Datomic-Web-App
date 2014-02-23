(ns dactic-server.controllers.util
  (:require [noir.validation :as validation :refer [get-errors errors?]]))

(defn errors-in-ctx
  [ctx]
  (select-keys ctx [:errors]))

(defn create-errors-vec
  [key-vec & optfn]
  ;;The booleans answer the question "allowed?"
  (if (apply errors? key-vec)
      [false {:errors
            (->> (filter #(not (= nil (get-errors %))) key-vec)
                 (mapcat (fn [x] [x (get-errors x)]))
                 (apply array-map ))
            :representation {:media-type "application/json"}
            }]
      (if optfn ((first optfn)) true)))

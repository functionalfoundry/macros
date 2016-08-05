(ns workflo.macros.entity.datascript
  (:require [datascript.core :as d]
            [workflo.macros.entity.schema :as es]))

(defn attr-schema
  [attr-opts]
  (reduce (fn [opts opt]
            (cond-> opts
              (= :ref opt)
              (assoc :db/valueType :db.type/ref)
              (= :many opt)
              (assoc :db/cardinality :db.cardinality/many)
              (= :unique-value opt)
              (assoc :db/unique :db.unique/value)
              (= :unique-identity opt)
              (assoc :db/unique :db.unique/identity)))
          {} attr-opts))

(defn entity-schema
  "Returns the DataScript schema for an entity."
  [entity]
  (reduce (fn [schema [attr-name attr-opts]]
            (let [aschema (attr-schema attr-opts)]
              (cond-> schema
                (not (empty? aschema))
                (assoc attr-name aschema))))
          {}
          (es/entity-schema entity)))

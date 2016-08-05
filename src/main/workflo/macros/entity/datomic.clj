(ns workflo.macros.entity.datomic
  (:require [datomic-schema.schema :as ds]
            [workflo.macros.entity.schema :as es]))

(defn attr->field
  [[attr-name [attr-type & attr-opts]]]
  [(name attr-name) [attr-type (set attr-opts)]])

(defn entity-schema
  "Returns the Datomic schema for an entity."
  [entity]
  (let [raw-schema (es/entity-schema entity)
        fields     (->> raw-schema
                        (filter #(not= :db/id (first %)))
                        (map attr->field)
                        (into {}))
        ds-schema  (ds/schema* (name (:name entity))
                               {:fields fields})]
    (ds/generate-schema [ds-schema])))

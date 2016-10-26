(ns workflo.macros.entity.datomic
  (:require [datomic-schema.schema :as ds]
            [workflo.macros.entity.schema :as es]))

(defn split-schema
  "Takes an entity schema and splits it into multiple
   entity schemas based on attribute prefixes. An entity
   with a spec like

       (s/keys :req [:foo/bar :baz/ruux])

   would yield an entity schema like

       {:foo/bar [...]
        :baz/ruux [...]}

   and would be split into

       {:foo {:foo/bar [...]}
        :baz {:bar/ruux [...]}}"
  [schema]
  (reduce (fn [ret [attr-name attr-def]]
            (let [prefix (keyword (namespace attr-name))]
              (if (nil? prefix)
                (throw (IllegalArgumentException.
                        (str "Unqualified attribute not supported "
                             "when generating a Datomic schema: "
                             attr-name))))
              (update ret prefix
                      (fn [m]
                        (assoc (or m {}) attr-name attr-def)))))
          {} schema))

(defn attr->field
  "Returns an attribute name and definition pair. Returns
   a pair suitable to be passed to datomic-schema's `schema*`
   function as a field."
  [[attr-name [attr-type & attr-opts]]]
  [(name attr-name) [attr-type (set attr-opts)]])

(defn datomic-schema
  "Generates a Datomic schema from an entity schema with
   a specific prefix."
  [[prefix schema]]
  (ds/schema* (name prefix) {:fields (map attr->field schema)}))

(defn entity-schema
  "Returns the Datomic schema for an entity."
  [entity]
  (let [raw-schema      (es/entity-schema entity)
        prefix-schemas  (->> raw-schema
                             (filter #(not= :db/id (first %)))
                             (into {})
                             (split-schema))
        datomic-schemas (map datomic-schema prefix-schemas)]
    (ds/generate-schema datomic-schemas)))

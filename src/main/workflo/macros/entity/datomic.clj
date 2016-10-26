(ns workflo.macros.entity.datomic
  (:require [datomic-schema.schema :as ds]
            [workflo.macros.entity.schema :as es]
            [workflo.macros.util.misc :refer [drop-keys]]))

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

(defn normalize-attr
  "Normalizes a Datomic schema attribute by converting
   it to a map if it's of the form `[:db/add ...]`."
  [attr]
  (if (vector? attr)
    (do
      (assert (= :db/add (first attr)))
      (let [kvs (into [:db/id] (rest attr))]
        (apply hash-map kvs)))
    attr))

(defn- merge-attr-schema
  "\"Merges\" two schemas for an attribute with the same name
   by throwing an exception if they are different and the
   first is not nil, and otherwise picking the second."
  [a b]
  (if (nil? a)
    b
    (let [a-without-id (drop-keys a [:db/id])
          b-without-id (drop-keys b [:db/id])]
      (if (= a-without-id b-without-id)
        b
        (throw (Exception.
                (str "Conflicting schemas for attribute: "
                     (pr-str a-without-id) " != "
                     (pr-str b-without-id))))))))

(defn merge-schemas
  "Merge multiple Datomic schemas so that there are no
   conflicting attributes. The default behavior is to throw
   an exception if two schemas for the same attribute are
   different."
  ([schemas]
   (merge-schemas schemas merge-attr-schema))
  ([schemas merge-fn]
   (transduce (map normalize-attr)
              (completing (fn [ret attr]
                            (update ret (:db/ident attr) merge-fn attr))
                          vals)
              {} (apply concat schemas))))

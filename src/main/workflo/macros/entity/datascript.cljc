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

(defn merge-attr-schema
  "\"Merges\" two schemas for an attribute with the same name
   by throwing an exception if they are different and the
   first is not nil, and otherwise picking the second."
  [[attr-name prev-schema] [_ next-schema]]
  (if (nil? prev-schema)
    next-schema
    (if (= prev-schema next-schema)
      next-schema
      (let [err-msg (str "Conflicting schemas for attribute \""
                         attr-name "\": "
                         (pr-str prev-schema) " != "
                         (pr-str next-schema))]
        (throw #?(:cljs (js/Error. err-msg)
                  :clj  (Exception. err-msg)))))))

(defn merge-schemas
  "Merge multiple DataScript schemas so that there are no
   conflicting attributes. The default behavior is to throw
   an exception if two schemas for the same attribute are
   different."
  ([schemas]
   (merge-schemas schemas merge-attr-schema))
  ([schemas merge-fn]
   (reduce (fn [ret [attr-name attr-schema]]
             (update ret attr-name
                     (fn [existing-schema]
                       (merge-fn [attr-name existing-schema]
                                 [attr-name attr-schema]))))
           {} (apply concat schemas))))

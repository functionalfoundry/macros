(ns workflo.macros.entity.schema
  (:refer-clojure :exclude [keys])
  (:require [clojure.set :refer [intersection]]
            [clojure.spec :as s]
            [workflo.macros.specs.entity]
            [workflo.macros.specs.types :as types]
            [workflo.macros.util.misc :refer [val-after]]))

;;;; Helpers

(defn type-spec? [spec] (keyword? spec))
(defn and-spec? [spec] (and (seq? spec) (= 'and (first spec))))
(defn keys-spec? [spec] (and (seq? spec) (= 'keys (first spec))))
(defn enum-spec? [spec] (= :workflo.macros.specs.types/enum spec))
(defn entity-ref-spec? [spec]
  (and (seq? spec) (= 'entity-ref (first spec))))

(defn simple-entity?
  "Returns true if the entity is is simple, that is, its spec
   refers to a single value, not a map with keys."
  [entity]
  (let [desc (cond-> (:spec entity)
               (not (type-spec? (:spec entity)))
               s/describe)]
    (or (type-spec? desc)
        (entity-ref-spec? desc)
        (and (and-spec? desc)
             (empty? (filter keys-spec? desc)))
        (and (and-spec? desc)
             (empty? (filter keys-spec? desc))))))

;;;; Schemas from value or type specs

(defn type-spec-schema
  [spec]
  (case spec
    ;; Types
    :workflo.macros.specs.types/id [:long]
    :workflo.macros.specs.types/keyword [:keyword]
    :workflo.macros.specs.types/string [:string]
    :workflo.macros.specs.types/boolean [:boolean]
    :workflo.macros.specs.types/long [:long]
    :workflo.macros.specs.types/bigint [:bigint]
    :workflo.macros.specs.types/float [:float]
    :workflo.macros.specs.types/double [:double]
    :workflo.macros.specs.types/bigdec [:bigdec]
    :workflo.macros.specs.types/instant [:instant]
    :workflo.macros.specs.types/uuid [:uuid]
    :workflo.macros.specs.types/bytes [:bytes]
    :workflo.macros.specs.types/enum [:enum]
    :workflo.macros.specs.types/ref [:ref]
    :workflo.macros.specs.types/ref-many [:ref :many]

    ;; Type options
    :workflo.macros.specs.types/unique-value [:unique-value]
    :workflo.macros.specs.types/unique-identity [:unique-identity]
    :workflo.macros.specs.types/indexed [:indexed]
    :workflo.macros.specs.types/fulltext [:fulltext]
    :workflo.macros.specs.types/no-history [:nohistory]
    :workflo.macros.specs.types/component [:component]))

(defn enum-values-from-and-spec
  [spec]
  (let [set-specs (filter set? spec)]
    (when-not (empty? set-specs)
      (->> set-specs
           (apply intersection)
           (into [])))))

(defn and-spec-schema
  [spec]
  (let [type-specs  (filter type-spec? spec)
        schemas     (->> (map type-spec-schema type-specs)
                         (apply concat)
                         (into []))
        enum-values (when (some enum-spec? type-specs)
                      (enum-values-from-and-spec spec))]
    (cond-> schemas
      enum-values (conj enum-values))))

(defn entity-ref-spec-schema
  [spec]
  (cond-> [:ref]
    (val-after spec :many?) (conj :many)))

(defn value-spec-schema
  [spec]
  (let [desc (cond-> spec
               (not (type-spec? spec))
               s/describe)]
    (cond->> desc
      (type-spec? desc) (type-spec-schema)
      (and-spec? desc) (and-spec-schema)
      (entity-ref-spec? desc) (entity-ref-spec-schema))))

;;;;;; Schemas from entity specs

(defn key-specs
  [keys]
  (zipmap keys (mapv s/get-spec keys)))

(defn key-schemas
  [kspecs]
  (zipmap (clojure.core/keys kspecs)
          (mapv value-spec-schema (vals kspecs))))

(defn types-entity-spec-schema
  ([entity type-specs]
   (types-entity-spec-schema entity type-specs nil))
  ([entity type-specs enum-values]
   {(keyword (:name entity))
    (cond-> (->> type-specs
                 (map type-spec-schema)
                 (apply concat)
                 (into []))
      enum-values (conj enum-values))}))

(defn keys-entity-spec-schema
  [entity spec]
  (let [req-key-schemas (-> (or (val-after spec :req) [])
                            (key-specs)
                            (key-schemas))
        opt-key-schemas (-> (or (val-after spec :opt) [])
                            (key-specs)
                            (key-schemas))]
    (merge req-key-schemas opt-key-schemas)))

(defn and-entity-spec-schema
  [entity spec]
  (let [keys-spec (first (filter keys-spec? spec))
        type-specs (filter type-spec? spec)
        enum-values (when (some enum-spec? type-specs)
                      (enum-values-from-and-spec spec))]
    (cond
      keys-spec (keys-entity-spec-schema entity keys-spec)
      type-specs (types-entity-spec-schema entity type-specs
                                           enum-values))))

(defn entity-ref-entity-spec-schema
  [entity spec]
  {(keyword (:name entity)) (entity-ref-spec-schema spec)})

(defn entity-spec-schema
  [entity spec]
  (cond
    (type-spec? spec) (types-entity-spec-schema entity [spec])
    (and-spec? spec) (and-entity-spec-schema entity spec)
    (keys-spec? spec) (keys-entity-spec-schema entity spec)
    (entity-ref-spec? spec) (entity-ref-entity-spec-schema entity spec)))

;;;; Schemas from entities

(defn entity-schema
  [entity]
  (let [desc (cond-> (:spec entity)
               (not (type-spec? (:spec entity)))
               s/describe)]
    (entity-spec-schema entity desc)))

(defn matching-entity-schemas
  [entities-map name-pattern]
  (->> entities-map
       (vals)
       (filter #(->> % :name str (re-matches name-pattern)))
       (map entity-schema)
       (apply merge)))

;;;; Required, optional and all keys in the entity

(defn keys-spec-keys
  [entity spec]
  (let [req (val-after spec :req)
        opt (val-after spec :opt)]
    (cond-> {}
      req (assoc :required req)
      opt (assoc :optional opt))))

(defn and-spec-keys
  [entity spec]
  (let [keys-spec (first (filter keys-spec? spec))]
    (some->> keys-spec
      (keys-spec-keys entity))))

(defn spec-keys
  [entity spec]
  (or (cond
        (and-spec? spec) (and-spec-keys entity spec)
        (keys-spec? spec) (keys-spec-keys entity spec))
      {}))

(defn keys
  [entity]
  (let [desc (cond-> (:spec entity)
               (not (type-spec? (:spec entity)))
               s/describe)]
    (spec-keys entity desc)))

(defn required-keys
  [entity]
  (or (:required (keys entity)) []))

(defn optional-keys
  [entity]
  (or (:optional (keys entity)) []))

;;;; Entity ref(erences)

(defn entity-refs
  [entity]
  (if (simple-entity? entity)
    (when (entity-ref-spec? (s/describe (:spec entity)))
      (types/entity-ref-info (:spec entity)))
    (letfn [(key-specs [entity]
              (let [keys (mapcat second (keys entity))]
                (zipmap keys (map s/get-spec keys))))]
      (let [ref-specs (filter (comp entity-ref-spec? s/describe second)
                              (key-specs entity))]
        (zipmap (map first ref-specs)
                (map (comp types/entity-ref-info second)
                     ref-specs))))))

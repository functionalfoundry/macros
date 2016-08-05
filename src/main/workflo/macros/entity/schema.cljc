(ns workflo.macros.entity.schema
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [workflo.macros.entity :as e]
            [workflo.macros.specs.entity]))

;;;; Helpers

(defn val-after
  [coll x]
  (loop [coll coll]
    (if (= x (first coll))
      (first (rest coll))
      (when (not (empty? (rest coll)))
        (recur (rest coll))))))

(defn type-spec? [spec] (keyword? spec))
(defn and-spec? [spec] (and (seq? spec) (= 'and (first spec))))
(defn keys-spec? [spec] (and (seq? spec) (= 'keys (first spec))))

;;;; Schemas from value or type specs

(defn type-spec-schema
  [spec]
  (case spec
    ;; Types
    :workflo.macros.specs.types/id []
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

(defn and-spec-schema
  [spec]
  (let [type-specs (filter type-spec? spec)
        schemas    (map type-spec-schema type-specs)]
    (into [] (apply concat schemas))))

(defn value-spec-schema
  [spec]
  (let [desc (cond-> spec
               (not (type-spec? spec))
               s/describe)]
    (cond->> desc
      (type-spec? desc) (type-spec-schema)
      (and-spec? desc) (and-spec-schema))))

;;;;;; Schemas from entity specs

(defn key-specs
  [keys]
  (zipmap keys (mapv s/get-spec keys)))

(defn key-schemas
  [kspecs]
  (zipmap (keys kspecs)
          (mapv value-spec-schema (vals kspecs))))

(defn types-entity-spec-schema
  [entity type-specs]
  {(keyword (:name entity))
   (apply concat (map type-spec-schema type-specs))})

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
  (let [keys-spec  (first (filter keys-spec? spec))
        type-specs (filter type-spec? spec)]
    (cond
      keys-spec  (keys-entity-spec-schema entity keys-spec)
      type-specs (types-entity-spec-schema entity type-specs))))

(defn entity-spec-schema
  [entity spec]
  (cond
    (type-spec? spec) (types-entity-spec-schema entity [spec])
    (and-spec? spec)  (and-entity-spec-schema entity spec)
    (keys-spec? spec) (keys-entity-spec-schema entity spec)))

;;;; Schemas from entities

(defn entity-schema
  [entity]
  (let [desc (cond-> (:spec entity)
               (not (type-spec? (:spec entity)))
               s/describe)]
    (entity-spec-schema entity desc)))

(defn matching-entity-schemas
  [name-pattern]
  (->> (e/registered-entities)
       (vals)
       (filter #(->> % :name str (re-matches name-pattern)))
       (map entity-schema)
       (apply merge)))

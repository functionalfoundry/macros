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
      (recur (rest coll)))))

(defn type-spec? [spec] (keyword? spec))
(defn and-spec? [spec] (and (seq? spec) (= 'and (first spec))))
(defn keys-spec? [spec] (and (seq? spec) (= 'keys (first spec))))

;;;; Schemas from value or type specs

(defn type-spec-schema
  [spec]
  (case spec
    :workflo.macros.specs.types/id []
    :workflo.macros.specs.types/string [:string]
    :workflo.macros.specs.types/boolean [:boolean]))

(defn and-spec-schema
  [spec]
  (let [type-spec (first (filter type-spec? spec))]
    (type-spec-schema type-spec)))

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

(defn type-entity-spec-schema
  [entity spec]
  {(keyword (:name entity)) (type-spec-schema spec)})

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
  (let [sub-spec (or (first (filter keys-spec? spec))
                     (first (filter type-spec? spec)))]
    (cond->> sub-spec
      (type-spec? sub-spec) (type-entity-spec-schema entity)
      (keys-spec? sub-spec) (keys-entity-spec-schema entity))))

(defn entity-spec-schema
  [entity spec]
  (cond->> spec
    (type-spec? spec) (type-entity-spec-schema entity)
    (and-spec? spec) (and-entity-spec-schema entity)
    (keys-spec? spec) (keys-entity-spec-schema entity)))

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

(ns workflo.macros.entity.schema
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [workflo.macros.entity :as e]
            [workflo.macros.specs.entity]))

;;;; Schemas from type specs

(defn type-spec-schema
  [spec]
  (case spec
    :workflo.macros.specs.types/id []
    :workflo.macros.specs.types/string [:string]
    :workflo.macros.specs.types/boolean [:boolean]))

;;;; Schemas from value specs

(defmulti value-spec-schema (fn [[tag spec]] tag))

(defmethod value-spec-schema :and
  [[_ spec]])

(defmethod value-spec-schema :simple
  [[_ spec]]
  (type-spec-schema spec))

;;;; Schemas from entity specs

(defn key-specs
  [keys]
  (zipmap keys (mapv s/get-spec keys)))

(defn key-schemas
  [kspecs]
  (zipmap (keys kspecs)
          (mapv type-spec-schema (vals kspecs))))

(defmulti entity-spec-schema (fn [entity [tag spec]] tag))

(defmethod entity-spec-schema :and
  [entity [_ spec]]
  (entity-spec-schema entity (:spec spec)))

(defmethod entity-spec-schema :keys
  [entity [_ spec]]
  (let [req-key-schemas (-> (or (:keys (:required spec)) [])
                            (key-specs)
                            (key-schemas))
        opt-key-schemas (-> (or (:keys (:optional spec)) [])
                            (key-specs)
                            (key-schemas))]
    (merge req-key-schemas opt-key-schemas)))

(defmethod entity-spec-schema :value
  [entity [_ spec]]
  {(keyword (:name entity)) (value-spec-schema spec)})

;;;; Schemas from entities

(defn entity-schema
  [entity]
  (entity-spec-schema entity (:conforming-spec entity)))

(ns workflo.macros.entity
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.specs.entity]))

;;;; Entity registry

(defonce ^:private +registry+ (atom {}))

(defn register-entity!
  [entity-name env]
  (let [entity-sym     (util/unqualify entity-name)
        definition-sym (util/prefix-form-name 'definition entity-sym)
        definition     (symbol (str (ns-name *ns*))
                               (str definition-sym))]
    (swap! +registry+ assoc entity-name definition)))

(defn registered-entities
  []
  @+registry+)

(defn resolve-entity-sym
  [entity-name]
  (let [entity-sym (get @+registry+ entity-name)]
    (when (nil? entity-sym)
      (let [err-msg (str "Failed to resolve entity '" entity-name "'")]
        (throw (Exception. err-msg))))
    entity-sym))

(defn resolve-entity
  [entity-name]
  @(resolve (resolve-entity-sym entity-name)))

(defn authenticate
  [entity-name data])

(defn validate
  [entity-name data])

(defn schema
  [entity-name]
  (let [entity (resolve-entity entity-name)]
    (:schema entity)))

;;;; The defentity macro

(s/fdef defentity*
  :args :workflo.macros.specs.entity/defentity-args
  :ret  ::s/any)

(defn defentity*
  ([name forms]
   (defentity* name forms nil))
  ([name forms env]
   `(do
      )))

(defmacro defentity
  [name & forms]
  (register-entity! name &env)
  (defentity* name forms &env))

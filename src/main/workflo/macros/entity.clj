(ns workflo.macros.entity
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.command.util :as util]
            [workflo.macros.specs.entity]))

;;;; Configuration

(defonce ^:private +configuration+
  (atom {:auth-query nil}))

(defn configure!
  "Configures how entities are created with defentity and how aspects
   like authorization are performed against them. Supports the
   following options:

   :auth-query - a function that takes a parsed query and entity data;
                 this query result from this function is then passed to
                 the entity's auth function to perform authorization."
  [{:keys [auth-query] :as options}]
  (swap! +configuration+ assoc
         :auth-query auth-query))

(defn get-config
  "Returns the configuration for a given configuration key, e.g.
   :auth-query."
  [key]
  (@+configuration+ key))

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
  [entity data])

(defn validate
  [entity-or-name data]
  (letfn [(validate* [[spec-name spec] data]
            (or (s/valid? spec data)
                (throw (Exception.
                        (format "%s failed: %s"
                                (string/capitalize (name spec-name))
                                (s/explain-str spec data))))))]
    (let [entity (cond-> entity-or-name
                   (symbol? entity-or-name)
                   resolve-entity)]
      (->> [:schema :validation]
           (select-keys entity)
           (map #(validate* % data))
           (every? true?)))))

(defn validation
  [entity]
  (:validation entity))

(defn schema
  [entity]
  (:schema entity))

;;;; Utilities

(defn prefixed-form-name
  [form prefix]
  (symbol (str (ns-name *ns*))
          (str (util/prefix-form-name (:form-name form) prefix))))

;;;; The defentity macro

(s/fdef defentity*
  :args :workflo.macros.specs.entity/defentity-args
  :ret  ::s/any)

(defn defentity*
  ([name forms]
   (defentity* name forms nil))
  ([name forms env]
   (let [args-spec       :workflo.macros.specs.entity/defentity-args
         args            (if (s/valid? args-spec [name forms])
                           (s/conform args-spec [name forms])
                           (throw (Exception.
                                   (s/explain-str args-spec
                                                  [name forms]))))
         description     (:description (:forms args))
         auth            (:auth (:forms args))
         validation      (:validation (:forms args))
         schema          (:schema (:forms args))
         forms           (-> (:forms args)
                             (select-keys [:auth :validation :schema])
                             (vals))
         name-sym        (util/unqualify name)
         forms-map       (zipmap (map (comp keyword :form-name) forms)
                                 (map #(prefixed-form-name % name-sym)
                                      forms))
         auth-form       (when auth
                           `((~'defn ~(util/prefix-form-name 'auth
                                                             name-sym)
                              []
                              ~@(:form-body auth))))
         validation-form (when validation
                           `((~'def ~(util/prefix-form-name 'validation
                                                            name-sym)
                              ~@(:form-body validation))))
         schema-form     (when schema
                           `((~'def ~(util/prefix-form-name 'schema
                                                            name-sym)
                              ~@(:form-body schema))))
         definition      `(~'def ~(util/prefix-form-name 'definition
                                                         name-sym)
                           ~forms-map)]
     `(do
        ~@auth-form
        ~@validation-form
        ~@schema-form
        ~definition))))

(defmacro defentity
  [name & forms]
  (register-entity! name &env)
  (defentity* name forms &env))

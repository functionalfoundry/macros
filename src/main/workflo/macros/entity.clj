(ns workflo.macros.entity
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.entity.refs :as refs]
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.entity]
            [workflo.macros.specs.query :as specs.query]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

;;;; Configuration options for the defentity macro

(defconfig entity
  ;; Configures how entities are created with defentity and how aspects
  ;; like authorization are performed against them. Supports the
  ;; following options:
  ;;
  ;; :auth-query - a function that takes an environment and a parsed query
  ;;               the query result from this function is then passed
  ;;               to the entity's auth function to perform authorization.
  {:auth-query nil})

;;;; Entity registry

(declare entity-registered)
(declare entity-unregistered)

(defregistry entity (fn [event entity-name]
                      (case event
                        :register   (entity-registered entity-name)
                        :unregister (entity-unregistered entity-name))))

(defn entity-registered
  [entity-name]
  (let [entity-def (resolve-entity entity-name)]
    (refs/register-entity-refs! entity-name entity-def)))

(defn entity-unregistered
  [entity-name]
  (refs/unregister-entity-refs! entity-name))

(defn entity-refs
  [entity-name]
  (refs/entity-refs entity-name))

(defn entity-backrefs
  [entity-name]
  (refs/entity-backrefs entity-name))

;;;; Validation

(defn validate
  [entity-or-name data]
  (let [entity (cond-> entity-or-name
                 (symbol? entity-or-name)
                 resolve-entity)
        spec   (:spec entity)]
    (or (s/valid? spec data)
        (throw (Exception.
                (format "Validation of %s entity data failed: %s"
                        (:name entity)
                        (s/explain-str spec data)))))))

;;;; Authorization

(defn authorized?
  [entity-or-name env entity-id viewer-id]
  (let [entity  (cond-> entity-or-name
                  (symbol? entity-or-name)
                  resolve-entity)
        auth-fn (:auth entity)]
    (if auth-fn
      (let [auth-data    {:entity-id entity-id
                          :viewer-id viewer-id}
            query-result (when-let [query-hook (some-> (get-entity-config :auth-query)
                                                       (partial env))]
                           (some-> (:auth-query entity)
                                   (q/bind-query-parameters auth-data)
                                   (query-hook)))]
        (auth-fn query-result entity-id viewer-id))
      true)))

;;;; Look up entity definitions for entity data

(defn- entity-attrs [entity]
  (concat (schema/required-keys entity)
          (schema/optional-keys entity)))

(def memoized-entity-attrs
  (memoize entity-attrs))

(defn entity-for-attr [attr]
  (some (fn [[_ entity]]
          (when (some #{attr} (memoized-entity-attrs entity))
            entity))
        (registered-entities)))

(def memoized-entity-for-attr
  (memoize entity-for-attr))

(defn entity-for-data [data]
  (some (fn [[attr _]]
          (memoized-entity-for-attr attr))
        data))

;;;; The defentity macro

(s/fdef defentity*
  :args :workflo.macros.specs.entity/defentity-args
  :ret  any?)

(defn defentity*
  ([name forms]
   (defentity* name forms nil))
  ([name forms env]
   (let [args-spec         :workflo.macros.specs.entity/defentity-args
         args              (if (s/valid? args-spec [name forms])
                             (s/conform args-spec [name forms])
                             (throw (Exception.
                                     (s/explain-str args-spec
                                                    [name forms]))))
         description       (:description (:forms args))
         hints             (:form-body (:hints (:forms args)))
         target-cljs?      (boolean (:ns env))
         auth-query        (when-not target-cljs?
                             (some-> args :forms :auth-query :form-body))
         parsed-auth-query (when (and auth-query (not target-cljs?))
                             (q/parse auth-query))
         auth              (when-not target-cljs?
                             (some-> args :forms :auth :form-body))
         validation        (:validation (:forms args))
         spec              (:form-body (:spec (:forms args)))
         name-sym          (unqualify name)
         forms             (-> (:forms args)
                               (select-keys [:spec])
                               (vals)
                               (cond->
                                   true        (conj {:form-name 'name})
                                   description (conj {:form-name 'description})
                                   hints       (conj {:form-name 'hints})
                                   auth-query  (conj {:form-name 'auth-query})
                                   auth        (conj {:form-name 'auth})))
         def-sym           (f/qualified-form-name 'definition name-sym)]
     `(do
        ~(f/make-def name-sym 'name `'~name)
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when hints
            `(~(f/make-def name-sym 'hints hints)))
        ~(f/make-def name-sym 'spec spec)
        ~@(when auth-query
            `((~'def ~(f/prefixed-form-name 'auth-query name-sym)
               '~parsed-auth-query)))
        ~@(when auth
            `(~(f/make-defn name-sym 'auth
                 '[auth-query-result entity-id viewer-id]
                 (if auth-query
                   `((workflo.macros.bind/with-query-bindings
                       ~parsed-auth-query ~'auth-query-result
                       ~@auth))
                   auth))))
        ~@(when validation
            `(~(f/make-def name-sym 'validation
                 (:form-body validation))))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-entity! '~name ~def-sym)))))

(defmacro defentity
  [name & forms]
  (defentity* name forms &env))

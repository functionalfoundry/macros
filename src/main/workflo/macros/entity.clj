(ns workflo.macros.entity
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.entity]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

;;;; Configuration options for the defentity macro

(defconfig entity
  ;; Configures how entities are created with defentity and how aspects
  ;; like authorization are performed against them. Supports the
  ;; following options:
  ;;
  ;; :auth-query - a function that takes a parsed query and entity data;
  ;;               this query result from this function is then passed
  ;;               to the entity's auth function to perform
  ;;               authorization.
  {:auth-query nil})

;;;; Entity registry

(defregistry entity)

;;;; Authentication

(defn authenticate
  [entity-or-name data]
  (let [entity  (cond-> entity-or-name
                  (symbol? entity-or-name)
                  resolve-entity)
        auth-fn (:auth entity)]
    (if auth-fn
      (let [auth-query   (some-> entity :auth-query
                                 (q/bind-query-parameters data))
            query-result (some-> (get-entity-config :auth-query)
                                 (apply [auth-query data]))]
        (auth-fn query-result))
      true)))

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

;;;; The defentity macro

(s/fdef defentity*
  :args :workflo.macros.specs.entity/defentity-args
  :ret  any?)

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
         auth-query      (some-> (:auth-query auth) q/parse)
         query-keys      (some-> auth-query q/map-destructuring-keys)
         validation      (:validation (:forms args))
         spec            (:form-body (:spec (:forms args)))
         name-sym        (unqualify name)
         forms           (-> (:forms args)
                             (select-keys [:auth :spec])
                             (vals)
                             (cond->
                               true        (conj {:form-name 'name})
                               description (conj {:form-name
                                                  'description})
                               auth-query  (conj {:form-name
                                                  'auth-query})))
         def-sym         (f/qualified-form-name 'definition name-sym)]
     `(do
        ~(f/make-def name-sym 'name `'~name)
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when auth
            `(~(f/make-defn name-sym 'auth [{:keys query-keys}]
                 (:form-body auth))))
        ~@(when auth-query
            `((~'def ~(f/prefixed-form-name 'auth-query name-sym)
               '~auth-query)))
        ~@(when validation
            `(~(f/make-def name-sym 'validation
                 (:form-body validation))))
        ~(f/make-def name-sym 'spec spec)
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-entity! '~name ~def-sym)))))

(defmacro defentity
  [name & forms]
  (defentity* name forms &env))

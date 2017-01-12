(ns workflo.macros.entity
  (:require-macros [workflo.macros.entity :refer [defentity]])
  (:require [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.entity.refs :as refs]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]))

;;;; Entity configuration

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

;;;; Query utilities

(defn conform-and-parse
  [query]
  (q/conform-and-parse query))

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

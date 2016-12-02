(ns workflo.macros.entity
  (:require-macros [workflo.macros.entity :refer [defentity]])
  (:require [workflo.macros.entity.refs :as refs]
            [workflo.macros.registry :refer [defregistry]]))

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

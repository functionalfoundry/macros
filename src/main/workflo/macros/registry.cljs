(ns workflo.macros.registry
  (:require-macros [workflo.macros.registry :refer [defregistry]]))

(defn throw-registry-error
  [msg]
  (throw (js/Error. msg)))

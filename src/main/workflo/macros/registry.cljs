(ns workflo.macros.registry
  (:require-macros [workflo.macros.registry]))

(defn throw-registry-error
  [msg]
  (throw (js/Error. msg)))

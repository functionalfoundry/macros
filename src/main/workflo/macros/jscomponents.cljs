(ns workflo.macros.jscomponents
  (:require-macros [workflo.macros.jscomponents :refer [defjscomponents]])
  (:require [workflo.macros.util.string]))

(def camelize-keys workflo.macros.util.string/camelize-keys)

(ns workflo.macros.util.js
  (:require [clojure.string :as string]))

(defn sym->var-string
  "Converts a ClojureScript symbol to a JS variable string."
  [sym]
  (-> (str sym)
      (string/replace #"/" ".")
      (string/replace #"-" "_")))

(defn resolve
  "Resolves a ClojureScript symbol into the value of a JS
   variable or function."
  [sym]
  (js/eval (sym->var-string sym)))

(ns workflo.macros.view.util
  (:require [clojure.string :as string]
            [workflo.macros.util.string :refer [camel->kebab]]))

(defn factory-name
  [sym]
  (symbol (camel->kebab (name sym))))

(ns app-macros.util.string
  (:require [clojure.string :as str]))

(defn camel->kebab
  "Converts from camel case (e.g. Foo or FooBar) to kebab case
   (e.g. foo or foo-bar)."
  [s]
  (->> s
       (re-seq #"[A-Z][a-z0-9_-]*")
       (str/join "-")
       (str/lower-case)))

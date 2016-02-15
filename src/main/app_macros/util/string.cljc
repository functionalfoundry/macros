(ns app-macros.util.string
  (:require [clojure.string :as str]))

(defn camel->kebab
  "Converts from camel case (e.g. Foo or FooBar) to kebab case
   (e.g. foo or foo-bar)."
  [s]
  (str/lower-case (str/replace s #"(.+)([A-Z])" "$1-$2")))

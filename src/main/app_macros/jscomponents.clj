(ns app-macros.jscomponents
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [app-macros.util.string :refer [camel->kebab]]))

(defn defjscomponent*
  [module name]
  (let [fn-sym (symbol (camel->kebab name))
        js-sym (symbol "js" (str module "." name))]
    `(defn ~fn-sym [props# & children#]
       (.apply ~(symbol "js" "React.createElement") nil
               (into-array
                (cons ~js-sym
                      (cons (~'clj->js props#)
                            children#)))))))

(defn defjscomponents*
  [module]
  (let [filename   (str module ".json")
        components (-> filename io/resource slurp json/read-str)]
    `(do
       ~@(map (partial defjscomponent* module) components))))

(defmacro defjscomponents
  "Defines ClojureScript functions of the form

       (defn <component-name> [props & children]
         (js/React.createElement js/<module>.<ComponentName>
           (clj->js props)
           ... children ..))

   for all component names <ComponentName> listed in the file
   <module>.json in the classpath.

   This allows to integrate an entire JavaScript React component
   library into a ClojureScript project (e.g. using Om Next) with
   a single (defjscomponents ComponentLibraryName) expression and
   a ComponentLibraryName.json file."
  [module]
  (defjscomponents* module))

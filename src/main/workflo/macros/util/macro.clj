(ns workflo.macros.util.macro
  (:require [clojure.string :as string]
            [workflo.macros.query :refer [map-destructuring-keys]]
            [workflo.macros.util.string :refer [kebab->camel]]))

(defn definition-symbol
  "Returns a fully qualified definition symbol for a name.
   The definition symbol is the name under which the definition
   of e.g. a service or a command will be stored."
  [name]
  (symbol (str (ns-name *ns*))
          (str name)))

(defn record-symbol
  "Returns the symbol for a record. E.g. for `user`, it will
   return `User`."
  [name]
  (-> name str kebab->camel string/capitalize symbol))

(defn component-record-symbol
  "Returns the symbol for a component record. E.g. for `user`, it
   will return `UserComponent`."
  [name]
  (symbol (str (record-symbol name) 'Component)))

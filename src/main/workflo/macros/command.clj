(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.specs.query]))

(s/def ::command-name
  symbol?)

(s/def ::command-description
  string?)

(s/def ::command-query
  :workflo.macros.specs.query/query)

(s/def ::command-data-spec
  (s/with-gen
    s/spec?
    #(s/gen #{(s/spec symbol?)
              (s/spec map?)})))

(s/def ::command-inputs
  (s/tuple (s/? ::command-query)
           (s/? ::command-data-spec)))

(s/def ::command-form
  seq?)

(s/def ::command-implementation
  seq?)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [])))

(s/fdef defcommand
  :args (s/cat :name ::command-name
               :description (s/? ::command-description)
               :inputs (s/? ::command-inputs)
               :forms (s/* ::command-form)
               :implementation ::command-implementation)
  :ret  ::s/any)

(defmacro defcommand
  [name & forms]
  (defcommand* name forms &env))

(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [workflo.macros.specs.query]
            [workflo.macros.command.util :as util]))

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
  (s/tuple (s/? ::command-query) ::command-data-spec))

(s/def ::command-form
  seq?)

(s/def ::command-implementation
  seq?)

(s/def ::defcommand-args
  (s/cat :name ::command-name
         :forms
         (s/spec (s/cat :description (s/? ::command-description)
                        :inputs ::command-inputs
                        :forms (s/* ::command-form)
                        :implementation ::command-implementation))))

(s/fdef defcommand*
  :args ::defcommand-args
  :ret  ::s/any)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args        (s/conform ::defcommand-args [name forms])
         description (:description (:forms args))
         inputs      (:inputs (:forms args))
         cmd-forms   (:forms (:forms args))
         cmd-impl    (:implementation (:forms args))
         cache-query (when #(= 2 (count inputs))
                       (first inputs))
         data-spec   (last inputs)
         name-sym    (util/unqualify name)]
     (println "NAME" name name-sym)
     (println "DESCRIPTION" description)
     (println "CACHE-QUERY" cache-query)
     (println "DATA-SPEC" data-spec)
     (println "CMD-FORMS" cmd-forms)
     (println "CMD-IMPL" cmd-impl))))

(defmacro defcommand
  [name]
  (println "NAME" name)
  #_(defcommand* name forms &env))

(ns workflo.macros.specs.command
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def ::command-name
  symbol?)

(s/def ::command-description
  string?)

(s/def ::command-query
  :workflo.macros.specs.query/query)

(s/def ::command-data-spec
  (s/with-gen
    ::s/any
    #(s/gen #{symbol? map? vector?})))

(s/def ::command-inputs
  (s/spec (s/cat :command-query (s/? ::command-query)
                 :command-data-spec ::command-data-spec)))

(s/def ::command-form-name
  (s/with-gen
    (s/and symbol?
           #(not (some #{\/} (str %))))
    #(s/gen '#{workflows lifecycles foo bar foo-bar})))

(s/def ::command-form-body
  (s/* ::s/any))

(s/def ::command-form
  (s/spec (s/cat :form-name ::command-form-name
                 :form-body ::command-form-body)))

(s/def ::command-implementation
  ::s/any)

(s/def ::defcommand-args
  (s/cat :name ::command-name
         :forms
         (s/spec (s/cat :description (s/? ::command-description)
                        :inputs ::command-inputs
                        :forms (s/* ::command-form)
                        :implementation ::command-implementation))
         :env (s/? ::s/any)))

(s/def ::form-name
  ::command-form-name)

(s/def ::form-body
  ::command-form-body)

(s/def ::conforming-command-form
  (s/keys :req-un [::form-name ::form-body]))

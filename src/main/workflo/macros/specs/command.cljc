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

(s/def ::command-data-spec
  (s/with-gen
    ::s/any
    #(s/gen #{symbol? map? vector?})))

(s/def ::command-form-name
  (s/with-gen
    (s/and symbol?
           #(not (some #{\/} (str %))))
    #(s/gen '#{foo bar foo-bar})))

(s/def ::command-form-body
  (s/* ::s/any))

(s/def ::command-form
  (s/spec (s/cat :form-name ::command-form-name
                 :form-body ::command-form-body)))

(s/def ::command-query-form
  (s/spec (s/cat :form-name #{'query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::command-data-spec-form
  (s/spec (s/cat :form-name #{'data-spec}
                 :form-body ::command-data-spec)))

(s/def ::command-emit-form
  (s/spec (s/cat :form-name #{'emit}
                 :form-body ::command-form-body)))

(s/def ::defcommand-args
  (s/cat :name ::command-name
         :forms
         (s/spec (s/cat :description (s/? ::command-description)
                        :query (s/? ::command-query-form)
                        :data-spec (s/? ::command-data-spec-form)
                        :forms (s/* ::command-form)
                        :emit ::command-emit-form))
         :env (s/? ::s/any)))

(s/def ::form-name
  ::command-form-name)

(s/def ::form-body
  ::command-form-body)

(s/def ::conforming-command-form
  (s/keys :req-un [::form-name ::form-body]))

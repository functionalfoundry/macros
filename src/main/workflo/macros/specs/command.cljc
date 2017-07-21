(ns workflo.macros.specs.command
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [workflo.macros.specs.query]))

(s/def ::command-name
  symbol?)

(s/def ::command-description
  string?)

(s/def ::command-spec
  (s/with-gen
    any?
    #(s/gen #{symbol? map? vector?})))

(s/def ::command-form-name
  (s/with-gen
    (s/and symbol?
           #(not (some #{%} '[description
                              spec
                              query
                              auth-query
                              auth
                              emit]))
           #(not (some #{\/} (str %))))
    #(s/gen '#{foo bar foo-bar})))

(s/def ::command-form-body
  (s/* any?))

(s/def ::command-form
  (s/spec (s/cat :form-name ::command-form-name
                 :form-body ::command-form-body)))

(s/def ::command-hints
  (s/coll-of keyword? :kind vector? :min-count 1))

(s/def ::command-hints-form
  (s/spec (s/cat :form-name #{'hints}
                 :form-body ::command-hints)))

(s/def ::command-spec-form
  (s/spec (s/cat :form-name #{'spec}
                 :form-body ::command-spec)))

(s/def ::command-query-form
  (s/spec (s/cat :form-name #{'query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::command-auth-query-form
  (s/spec (s/cat :form-name #{'auth-query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::command-auth-form
  (s/spec (s/cat :form-name #{'auth}
                 :form-body ::command-form-body)))

(s/def ::command-emit-form
  (s/spec (s/cat :form-name #{'emit}
                 :form-body ::command-form-body)))

(s/def ::defcommand-args
  (s/cat :name ::command-name
         :forms
         (s/spec (s/cat :description (s/? ::command-description)
                        :hints (s/? ::command-hints-form)
                        :spec (s/? ::command-spec-form)
                        :query (s/? ::command-query-form)
                        :auth-query (s/? ::command-auth-query-form)
                        :auth (s/? ::command-auth-form)
                        :forms (s/* ::command-form)
                        :emit ::command-emit-form))
         :env (s/? any?)))

(s/def ::form-name
  ::command-form-name)

(s/def ::form-body
  ::command-form-body)

(s/def ::conforming-command-form
  (s/keys :req-un [::form-name ::form-body]))

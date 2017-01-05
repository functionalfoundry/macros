(ns workflo.macros.specs.entity
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

;;;; Specs for defentity arguments

(s/def ::entity-name
  symbol?)

(s/def ::entity-description
  string?)

(s/def ::entity-form-body
  (s/* any?))

(s/def ::entity-auth-query-form
  (s/spec (s/cat :form-name #{'auth-query}
                 :form-body any?)))

(s/def ::entity-auth-form
  (s/spec (s/cat :form-name #{'auth}
                 :form-body ::entity-form-body)))

(s/def ::entity-spec-form
  (s/spec (s/cat :form-name #{'spec}
                 :form-body any?)))

(s/def ::defentity-args
  (s/cat :name ::entity-name
         :forms (s/spec (s/cat :description (s/? ::entity-description)
                               :spec ::entity-spec-form
                               :auth-query (s/? ::entity-auth-query-form)
                               :auth (s/? ::entity-auth-form)))
         :env (s/? any?)))

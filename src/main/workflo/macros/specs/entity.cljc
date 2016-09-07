(ns workflo.macros.specs.entity
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

;;;; Specs for defentity arguments

(s/def ::entity-name
  symbol?)

(s/def ::entity-description
  string?)

(s/def ::entity-form-body
  (s/* ::s/any))

(s/def ::auth-form
  (s/spec (s/cat :form-name #{'auth}
                 :auth-query :workflo.macros.specs.query/query
                 :form-body ::entity-form-body)))

(s/def ::spec-form
  (s/spec (s/cat :form-name #{'spec}
                 :form-body ::s/any)))

(s/def ::defentity-args
  (s/cat :name ::entity-name
         :forms (s/spec (s/cat :description (s/? ::entity-description)
                               :auth (s/? ::auth-form)
                               :spec ::spec-form))
         :env (s/? ::s/any)))

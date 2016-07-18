(ns workflo.macros.specs.entity
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def ::entity-name
  symbol?)

(s/def ::entity-description
  string?)

(s/def ::entity-function-form-body
  (s/* ::s/any))

(s/def ::entity-form-body
  ::s/any)

(s/def ::auth-form
  (s/spec (s/cat :form-name #{'auth}
                 :auth-query :workflo.macros.specs.query/query
                 :form-body ::entity-function-form-body)))

(s/def ::validation-form
  (s/spec (s/cat :form-name #{'validation}
                 :form-body ::entity-form-body)))

(s/def ::schema-form
  (s/spec (s/cat :form-name #{'schema}
                 :form-body ::entity-form-body)))

(s/def ::defentity-args
  (s/cat :name ::entity-name
         :forms (s/spec (s/cat :description (s/? ::entity-description)
                               :auth (s/? ::auth-form)
                               :validation (s/? ::validation-form)
                               :schema ::schema-form))
         :env (s/? ::s/any)))

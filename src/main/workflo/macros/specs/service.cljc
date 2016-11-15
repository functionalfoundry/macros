(ns workflo.macros.specs.service
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def ::service-name
  symbol?)

(s/def ::service-description
  string?)

(s/def ::service-spec
  (s/with-gen
    any?
    #(s/gen #{symbol? map? vector?})))

(s/def ::service-dependencies
  (s/coll-of symbol? :kind vector?))

(s/def ::service-form-body
  (s/* any?))

(s/def ::service-dependencies-form
  (s/spec (s/cat :form-name #{'dependencies}
                 :form-body ::service-dependencies)))

(s/def ::service-replay?-form
  (s/spec (s/cat :form-name #{'replay?}
                 :form-body boolean?)))

(s/def ::service-query-form
  (s/spec (s/cat :form-name #{'query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::service-spec-form
  (s/spec (s/cat :form-name #{'spec}
                 :form-body ::service-spec)))

(s/def ::service-start-form
  (s/spec (s/cat :form-name #{'start}
                 :form-body ::service-form-body)))

(s/def ::service-stop-form
  (s/spec (s/cat :form-name #{'stop}
                 :form-body ::service-form-body)))

(s/def ::service-process-form
  (s/spec (s/cat :form-name #{'process}
                 :form-body ::service-form-body)))

(s/def ::defservice-args
  (s/cat :name ::service-name
         :forms
         (s/spec (s/cat :description (s/? ::service-description)
                        :dependencies (s/? ::service-dependencies-form)
                        :replay? (s/? ::service-replay?-form)
                        :query (s/? ::service-query-form)
                        :spec (s/? ::service-spec-form)
                        :start (s/? ::service-start-form)
                        :stop (s/? ::service-stop-form)
                        :process (s/? ::service-process-form)))
         :env (s/? any?)))

(ns workflo.macros.specs.service
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def ::service-name
  symbol?)

(s/def ::service-description
  string?)

(s/def ::service-data-spec
  (s/with-gen
    ::s/any
    #(s/gen #{symbol? map? vector?})))

(s/def ::service-form-body
  (s/* ::s/any))

(s/def ::service-query-form
  (s/spec (s/cat :form-name #{'query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::service-data-spec-form
  (s/spec (s/cat :form-name #{'data-spec}
                 :form-body ::service-data-spec)))

(s/def ::service-start-form
  (s/spec (s/cat :form-name #{'start}
                 :form-body ::service-form-body)))

(s/def ::service-stop-form
  (s/spec (s/cat :form-name #{'stop}
                 :form-body ::service-form-body)))

(s/def ::service-process-form
  (s/spec (s/cat :form-name #{'emit}
                 :form-body ::service-form-body)))

(s/def ::defservice-args
  (s/cat :name ::service-name
         :forms
         (s/spec (s/cat :description (s/? ::service-description)
                        :query (s/? ::service-query-form)
                        :data-spec (s/? ::service-data-spec-form)
                        :start (s/? ::service-start-form)
                        :stop (s/? ::service-stop-form)
                        :process ::service-process-form))
         :env (s/? ::s/any)))

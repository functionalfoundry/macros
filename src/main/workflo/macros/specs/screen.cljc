(ns workflo.macros.specs.screen
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def ::screen-name
  symbol?)

(s/def ::screen-description
  string?)

(s/def ::url-form
  (s/spec (s/cat :form-name #{'url}
                 :form-body string?)))

(s/def ::navigation-field
  (s/spec (s/cat :field-name symbol?
                 :field-value ::s/any)))

(s/def ::navigation-form-body
  (s/+ ::navigation-field))

(s/def ::navigation-form
  (s/spec (s/cat :form-name #{'navigation}
                 :form-body ::navigation-form-body)))

(s/def ::layout-form
  (s/spec (s/cat :form-name #{'layout}
                 :form-body (s/map-of keyword? ::s/any))))

(s/def ::defscreen-args
  (s/cat :name ::screen-name
         :forms (s/spec (s/cat :description (s/? ::screen-description)
                               :url ::url-form
                               :navigation ::navigation-form
                               :layout ::layout-form))
         :env (s/? ::s/any)))

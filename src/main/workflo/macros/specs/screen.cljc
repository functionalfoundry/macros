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

(s/def ::screen-form-name
  symbol?)

(s/def ::screen-form-body
  ::s/any)

(s/def ::screen-form
  (s/spec (s/cat :form-name ::screen-form-name
                 :form-body ::screen-form-body)))

(s/def ::layout-form
  (s/spec (s/cat :form-name #{'layout}
                 :form-body (s/map-of keyword? ::s/any))))

(s/def ::defscreen-args
  (s/cat :name ::screen-name
         :forms (s/spec (s/cat :description (s/? ::screen-description)
                               :url ::url-form
                               :forms (s/* ::screen-form)
                               :layout ::layout-form))
         :env (s/? ::s/any)))

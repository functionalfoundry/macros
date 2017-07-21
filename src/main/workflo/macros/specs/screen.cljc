(ns workflo.macros.specs.screen
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
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
  any?)

(s/def ::screen-form
  (s/spec (s/cat :form-name ::screen-form-name
                 :form-body ::screen-form-body)))

(s/def ::sections-form
  (s/spec (s/cat :form-name #{'sections}
                 :form-body (s/map-of keyword? any?))))

(s/def ::defscreen-args
  (s/cat :name ::screen-name
         :forms (s/spec (s/cat :description (s/? ::screen-description)
                               :url ::url-form
                               :forms (s/* ::screen-form)
                               :sections ::sections-form))
         :env (s/? any?)))

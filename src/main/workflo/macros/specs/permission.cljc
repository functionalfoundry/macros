(ns workflo.macros.specs.permission
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::permission-name (or symbol? keyword?))

(s/def ::permission-title string?)
(s/def ::permission-title-form
  (s/spec (s/cat :form-name #{'title}
                 :form-body ::permission-title)))

(s/def ::permission-description string?)
(s/def ::permission-description-form
  (s/spec (s/cat :form-name #{'description}
                 :form-body ::permission-description)))

(s/def ::defpermission-args
  (s/cat :name ::permission-name
         :forms (s/spec (s/cat :title ::permission-title-form
                               :description (s/? ::permission-description-form)))
         :env (s/? any?)))

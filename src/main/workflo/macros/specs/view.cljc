(ns workflo.macros.specs.view
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.query]))

(s/def ::view-name
  (s/with-gen
    util/capitalized-symbol?
    #(s/gen '#{Foo Bar FooBar})))

(s/def ::view-form-args
  (s/with-gen
    (s/coll-of symbol? :kind vector? :min-count 1)
    #(s/gen '#{[this] [props] [this props]})))

(s/def ::raw-view-form-name
  (s/with-gen
    (s/and symbol? #(= \. (first (str %))))
    #(s/gen '#{.do-this .do-that})))

(s/def ::raw-view-form
  (s/spec (s/cat :form-name ::raw-view-form-name
                 :form-args ::view-form-args
                 :form-body (s/* any?))))

(s/def ::regular-view-form
  (s/spec (s/cat :form-name (s/and symbol? #(not= \. (first (str %))))
                 :form-args (s/? ::view-form-args)
                 :form-body (s/* any?))))

(s/def ::view-form
  (s/alt :raw-view-form ::raw-view-form
         :regular-view-form ::regular-view-form))

(s/def ::view-query-form
  (s/spec (s/cat :form-name #{'query}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::view-computed-form
  (s/spec (s/cat :form-name #{'computed}
                 :form-body :workflo.macros.specs.query/query)))

(s/def ::defview-args
  (s/cat :name ::view-name
         :forms (s/spec (s/cat :query (s/? ::view-query-form)
                               :computed (s/? ::view-computed-form)
                               :forms (s/* ::view-form)))))

(ns workflo.macros.specs.view
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.query]))

(s/def ::view-name
  (s/with-gen
    util/capitalized-symbol?
    #(s/gen '#{Foo Bar FooBar})))

(s/def ::view-form-args
  #?(:cljs (s/and vector? (s/+ symbol?))
     :clj  (s/coll-of symbol? :kind vector? :min-count 1)))

(s/def ::view-form
  (s/spec (s/cat :form-name symbol?
                 :form-args (s/? ::view-form-args)
                 :form-body (s/* ::s/any))))

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

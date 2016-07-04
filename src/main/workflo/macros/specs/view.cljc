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

(s/def ::view-form
  #?(:cljs seq?
     :clj  (s/coll-of ::s/any :kind seq?)))

(s/def ::defview-args
  (s/cat :name ::view-name
         :forms
         (s/spec (s/cat :props (s/? :workflo.macros.specs.query/query)
                        :computed (s/? :workflo.macros.specs.query/query)
                        :forms (s/* ::view-form)))))

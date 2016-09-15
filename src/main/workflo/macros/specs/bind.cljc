(ns workflo.macros.specs.bind
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.parsed-query :as spq]))

(s/def ::path
  (s/coll-of ::spq/typed-property :gen-max 10))

(s/def ::paths
  (s/coll-of ::path :gen-max 10))

(ns workflo.macros.specs.bind
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [workflo.macros.specs.parsed-query :as spq]))

(s/def ::path
  (s/coll-of ::spq/typed-property :min-count 1 :gen-max 10))

(s/def ::paths
  (s/coll-of ::path :gen-max 10))

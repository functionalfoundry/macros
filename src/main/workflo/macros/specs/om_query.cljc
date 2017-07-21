(ns workflo.macros.specs.om-query
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [workflo.macros.specs.query :as q]
            [workflo.macros.query.util :as util]))

(s/def ::keyword
  keyword?)

(s/def ::link
  (s/tuple ::keyword :workflo.macros.specs.query/link-id))

(s/def ::component-query
  (s/cat :call '#{om.next/get-query}
         :component symbol?))

(s/def ::join-source
  (s/or :property ::keyword
        :link ::link))

(s/def ::join-target
  (s/or :query ::query
        :recursion ::q/join-recursion
        :component ::component-query))

(s/def ::join
  (s/and (s/map-of ::join-source ::join-target)
         util/one-item?))

(s/def ::regular-property
  (s/or :keyword ::keyword
        :link ::link
        :join ::join))

(s/def ::parameter-name
  keyword?)

(s/def ::parameter-path
  (s/coll-of ::parameter-name :kind vector? :min-count 1 :gen-max 3))

(s/def ::parameter-name-or-path
  (s/or :parameter-name ::parameter-name
        :parameter-path ::parameter-path))

(s/def ::parameters
  (s/map-of ::parameter-name-or-path any? :gen-max 5))

(s/def ::parameterized-property
  (s/spec (s/cat :list (s/? #{'clojure.core/list})
                 :property ::regular-property
                 :parameters ::parameters)))

(s/def ::property
  (s/or :regular ::regular-property
        :parameterized ::parameterized-property))

(s/def ::query
  (s/coll-of ::property
             :kind? vector? :min-count 1
             :gen-max 10))

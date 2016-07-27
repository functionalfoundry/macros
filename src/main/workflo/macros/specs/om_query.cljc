(ns workflo.macros.specs.om-query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
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
        :recursion :workflo.macros.specs.query/recursion
        :component ::component-query))

(s/def ::join
  (s/and (s/map-of ::join-source ::join-target)
         util/one-item?))

(s/def ::regular-property
  (s/or :keyword ::keyword
        :link ::link
        :join ::join))

(s/def ::parameters
  (s/map-of ::keyword ::s/any))

(s/def ::parameterized-property
  (s/spec (s/cat :quote #{'quote}
                 :parameterized-property
                 (s/spec (s/cat :property ::regular-property
                                :parameters ::parameters)))))

(s/def ::property
  (s/or :regular ::regular-property
        :parameterized ::parameterized-property))

(s/def ::query
  (s/with-gen
    (s/and vector? (s/+ ::property))
    #(gen/vector (s/gen ::property) 1 10)))

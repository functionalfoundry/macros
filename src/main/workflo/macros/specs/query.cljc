(ns workflo.macros.specs.query
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]))

;;;; Basics

(s/def ::property-name
  symbol?)

;;;; Simple properties

(s/def ::simple-property
  ::property-name)

;;;; Links

(s/def ::link-id
  any?)

(s/def ::link
  (s/tuple ::property-name ::link-id))

;;;; Joins

(s/def ::join-property
  (s/or :property-name ::property-name
        :link ::link))

(s/def ::model-name
  (s/with-gen
    util/capitalized-symbol?
    #(s/gen '#{User UserList Feature})))

(s/def ::model-join
  (s/map-of ::join-property ::model-name :count 1))

(s/def ::unlimited-recursion
  #{'... ''...})

(s/def ::limited-recursion
  (s/and integer? pos?))

(s/def ::recursion
  (s/or :unlimited ::unlimited-recursion
        :limited ::limited-recursion))

(s/def ::recursive-join
  (s/map-of ::join-property ::recursion :count 1))

(s/def ::properties-join
  (s/with-gen
    (s/map-of ::join-property ::query :count 1)
    #(gen/map (s/gen ::join-property)
              (s/gen '#{[user] [user [id name email]]})
              {:num-elements 1})))

(s/def ::join
  (s/or :model-join ::model-join
        :recursive-join ::recursive-join
        :properties-join ::properties-join))

;;;; Queries

(s/def ::property
  (s/or :simple ::simple-property
        :link ::link
        :join ::join))

(s/def ::aliased-property
  (s/cat :property ::property
         :alias (s/cat :as #{:as}
                       :alias ::property-name)))

(s/def ::property-or-aliased-property
  (s/alt :property ::property
         :aliased-property ::aliased-property))

(s/def ::property-group
  (s/coll-of ::property-or-aliased-property
             :kind vector? :min-count 1
             :gen-max 5))

(s/def ::nested-properties
  (s/cat :base ::property-name
         :children ::property-group))

(s/def ::regular-query
  (s/alt :property ::property-or-aliased-property
         :nested-properties ::nested-properties))

(s/def ::parameter-name
  symbol?)

(s/def ::parameter-value
  (s/with-gen
    any?
    gen/simple-type))

(s/def ::parameters
  (s/map-of ::parameter-name ::parameter-value
            :gen-max 5))

(s/def ::parameterized-query
  (s/with-gen
    (s/and list?
           (s/cat :regular-query-value ::regular-query
                  :parameters ::parameters))
    #(gen/fmap (fn [[query parameters]]
                 (apply list (conj query parameters)))
               (gen/tuple (s/gen ::regular-query)
                          (s/gen ::parameters)))))

(s/def ::query
  (s/with-gen
    (s/and vector?
           (s/+ (s/alt :regular-query ::regular-query
                       :parameterized-query ::parameterized-query)))
    #(gen/fmap util/combine-properties-and-groups
               (gen/vector (gen/one-of
                            [(s/gen ::property)
                             (s/gen ::nested-properties)
                             (s/gen ::parameterized-query)])
                           1 5))))

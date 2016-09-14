(ns workflo.macros.specs.conforming-query
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.query]))

(s/def ::simple
  (s/tuple #{:simple} :workflo.macros.specs.query/property-name))

(s/def ::link
  (s/tuple #{:link} :workflo.macros.specs.query/link))

(s/def ::join-source
  (s/or :simple ::simple
        :link ::link))

(s/def ::join-properties-value
  (s/with-gen
    (s/map-of ::join-source ::query :count 1)
    #(gen/map (s/gen ::join-source)
              (gen/vector
               (s/gen #{[:property [:simple 'a]]
                        [:property [:simple 'b]]
                        [:prefixed-properties
                         {:base 'a
                          :children [[:property [:simple 'b]]
                                     [:property [:simple 'c]]]}]
                        [:property [:link '[a _]]]})
               1 10)
              {:num-elements 1})))

(s/def ::join-properties
  (s/tuple #{:properties} ::join-properties-value))

(s/def ::unlimited-join-recursion
  (s/tuple #{:unlimited} #{'...}))

(s/def ::limited-join-recursion
  (s/tuple #{:limited} (s/and int? pos?)))

(s/def ::join-recursion-value
  (s/map-of ::join-source
            (s/or :unlimited ::unlimited-join-recursion
                  :limited ::limited-join-recursion)
            :count 1))

(s/def ::join-recursion
  (s/tuple #{:recursive} ::join-recursion-value))

(s/def ::join-view-value
  (s/map-of ::join-source
            (s/tuple #{:view} :workflo.macros.specs.query/view-name)
            :count 1))

(s/def ::join-view
  (s/tuple #{:view} ::join-view-value))

(s/def ::join
  (s/tuple #{:join} (s/or :properties ::join-properties
                          :recursion ::join-recursion
                          :view ::join-view)))

(s/def ::property-value
  (s/or :simple ::simple
        :link ::link
        :join ::join))

(s/def ::property
  (s/tuple #{:property} ::property-value))

(s/def :aliased-property-value/property
  ::property-value)

(s/def :aliased-property-value/alias
  :workflo.macros.specs.query/property-name)

(s/def :aliased-property-value/as
  #{:as})

(s/def ::aliased-property-value
  (s/keys :req-un [:aliased-property-value/property
                   :aliased-property-value/alias]
          :opt-un [:aliased-property-value/as]))

(s/def ::aliased-property
  (s/tuple #{:aliased-property} ::aliased-property-value))

(s/def :prefixed-properties-value/base
  :workflo.macros.specs.query/property-name)

(s/def :prefixed-properties-value/children
  (s/with-gen
    ::query
    #(gen/vector
      (s/gen (s/or :property ::property
                   :aliased-property ::aliased-property
                   :parameterization ::parameterization))
      1 10)))

(s/def ::prefixed-properties-value
  (s/keys :req-un [:prefixed-properties-value/base
                   :prefixed-properties-value/children]))

(s/def ::prefixed-properties
  (s/tuple #{:prefixed-properties} ::prefixed-properties-value))

(s/def :parameterization-value/query
  (s/or :property ::property
        :aliased-property ::aliased-property))

(s/def :parameterization-value/parameters
  :workflo.macros.specs.query/parameters)

(s/def ::parameterization-value
  (s/keys :req-un [:parameterization-value/query
                   :parameterization-value/parameters]))

(s/def ::parameterization
  (s/tuple #{:parameterization} ::parameterization-value))

;; Workaround for extra [] around [:aliased-property ...] and
;; [:prefixed-properties ...] in output of conform (JIRA issue
;; CLJ-2003)
(s/def ::bug-vector
  (s/coll-of (s/or :property ::property
                   :aliased-property ::aliased-property
                   :prefixed-properties ::prefixed-properties)
             :kind vector? :count 1))

(s/def ::query-value
  (s/or :property ::property
        :aliased-property ::aliased-property
        :prefixed-properties ::prefixed-properties
        :parameterization ::parameterization
        :bug-vector ::bug-vector))

(s/def ::query
  (s/coll-of ::query-value :kind vector?
             :min-count 1 :gen-max 10))

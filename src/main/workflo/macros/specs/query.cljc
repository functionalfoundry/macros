(ns workflo.macros.specs.query
  (:require [clojure.spec :as s]))

;;;; Simple properties

(s/def ::property-name
  symbol?)

;;;; Links

(s/def ::link-target
  any?)

(s/def ::link
  (s/tuple ::property-name ::link-target))

;;;; Joins

;; Join source

(s/def ::join-source
  (s/or :simple ::property-name
        :link ::link))

;; Recursive joins

(s/def ::join-recursion
  (s/or :unlimited #{'...}
        :limited (s/and int? pos?)))

(s/def ::recursive-join
  (s/map-of ::join-source ::join-recursion
            :count 1 :conform-keys true))

;; Property joins

(s/def ::properties-join
  (s/map-of ::join-source ::query
            :count 1 :conform-keys true))

;; Model joins

(s/def ::model-name
  symbol?)

(s/def ::join-model
  (s/or :model ::model-name))

(s/def ::model-join
  (s/map-of ::join-source ::join-model
            :count 1 :conform-keys true))

;; All possible joins

(s/def ::join
  (s/or :recursive ::recursive-join
        :properties ::properties-join
        :model ::model-join))

;;;; Individual properties, prefixed properties, aliased properties

(s/def ::property
  (s/or :simple ::property-name
        :link ::link
        :join ::join))

(s/def ::prefixed-properties
  (s/cat :base ::property-name
         :children ::query))

(s/def ::aliased-property
  (s/cat :property ::property
         :as #{:as}
         :alias ::property-name))

(s/def ::regular-query
  (s/or :property ::property))

(s/def ::parameters
  (s/map-of symbol? any?))

(s/def ::parameterization
  (s/and list?
         (s/cat :query ::regular-query
                :parameters ::parameters)))

(s/def ::query
  (s/and vector?
         (s/+ (s/alt :property ::property
                     :prefixed-properties ::prefixed-properties
                     :aliased-property ::aliased-property
                     :parameterization ::parameterization))))

(comment
  ;;;; Non-recursive queries

  ;; Regular property
  (s/conform ::query '[a])
  (s/conform ::query '[a b])
  (s/conform ::query '[a b c])

  ;; Link property
  (s/conform ::query '[[a _]])
  (s/conform ::query '[[a _] [b 1]])
  (s/conform ::query '[[a _] [b 1] [c :x]])

  ;; Join with property source
  (s/conform ::query '[{a [b]}])
  (s/conform ::query '[{a [b c]}])
  (s/conform ::query '[{a [b c]} d])
  (s/conform ::query '[{a [b c]} {d [e f]}])
  (s/conform ::query '[{a ...}])
  (s/conform ::query '[{a 5}])
  (s/conform ::query '[{a User}])

  ;; Join with link source
  (s/conform ::query '[{[a _] [b]}])
  (s/conform ::query '[{[a 1] [b c]}])
  (s/conform ::query '[{[a :x] [b c d]}])

  ;; Prefixed properties
  (s/conform ::query '[a [b]])
  (s/conform ::query '[a [b c]])
  (s/conform ::query '[a [b c] d])
  (s/conform ::query '[a [b c] d [e f]])

  ;; Aliased property
  (s/conform ::query '[a :as b])
  (s/conform ::query '[a :as b c :as d])

  ;; Aliased link
  (s/conform ::query '[[a _] :as b])
  (s/conform ::query '[[a 1] :as b])
  (s/conform ::query '[[a :x] :as b])
  (s/conform ::query '[[a _] :as b [c _] :as d])

  ;; Aliased join
  (s/conform ::query '[{a [b]} :as c])
  (s/conform ::query '[{a [b c]} :as d {e [f g]} :as h])

  ;; Aliased properties
  (s/conform ::query '[a [b :as c]])
  (s/conform ::query '[a [b :as c d :as e]])

  ;; Parameterization
  (s/conform ::query '[(a {b c})])
  (s/conform ::query '[(a {b c d e})])

  ;;;; Recursive queries

  ;; Join with sub-joins
  (s/conform ::query '[{users [db [id]
                               user [name]
                               {friends [db [id]
                                         user [name]]}]}])
  (s/conform ::query '[{users [{friends [{friends [db [id]]}]}]}])

  ;; Join with sub-links
  (s/conform ::query '[{users [db [id] [current-user _]]}])
  (s/conform ::query '[{users [user [name]
                               {[current-user _] [user [name]]}]}])

  ;; Join with sub-aliases
  (s/conform ::query '[{[user 1] [db [id :as db-id]
                                  name :as nm]}]))

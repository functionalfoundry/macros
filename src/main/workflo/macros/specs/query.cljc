(ns workflo.macros.specs.query
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]))

;;;; Simple properties

(s/def ::property-name
  symbol?)

;;;; Links

(s/def ::link-id
  (s/with-gen any? gen/simple-type))

(s/def ::link
  (s/tuple ::property-name ::link-id))

;;;; Joins

;; Join source

(s/def ::join-source
  (s/or :simple ::property-name
        :link ::link))

;; Recursive joins

(s/def ::join-recursion
  (s/or :unlimited #{'... ''...}
        :limited (s/and int? pos?)))

(s/def ::recursive-join
  (s/map-of ::join-source ::join-recursion
            :count 1 :conform-keys true))

;; Property joins

(s/def ::join-properties
  (s/with-gen
    ;; NOTE: The s/and here is a hack to make looking up ::query
    ;; work even though ::query is only defined later in this
    ;; namespace. It *should* work without it but it may be the
    ;; s/with-gen around it that makes it fail.
    (s/and ::query)
    #(s/gen '#{[user]
               [db [id] user [name email]]
               [[current-user _]]
               [{users [user [name email]]}]})))

(s/def ::properties-join
  (s/map-of ::join-source ::join-properties
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

(s/def ::prefixed-properties-value
  (s/with-gen
    ;; NOTE: The s/and here is a hack to make looking up ::query
    ;; work even though ::query is only defined later in this
    ;; namespace. It *should* work without it but it may be the
    ;; s/with-gen around it that makes it fail.
    (s/and ::query)
    #(s/gen '#{[id]
               [name email]
               [name email [current-user _]]})))

(s/def ::prefixed-properties
  (s/cat :base ::property-name
         :children ::prefixed-properties-value))

(s/def ::aliased-property
  (s/cat :property ::property
         :as #{:as}
         :alias ::property-name))

(s/def ::parameterization-query
  (s/alt :property ::property
         :aliased-property ::aliased-property))

(s/def ::parameter-name
  symbol?)

(s/def ::parameter-value
  (s/with-gen any? gen/simple-type))

(s/def ::parameters
  (s/map-of ::parameter-name ::parameter-value
            :gen-max 5))

(s/def ::parameterization
  (s/with-gen
    (s/and list?
           (s/cat :query ::parameterization-query
                  :parameters ::parameters))
    #(gen/fmap (fn [[query parameters]]
                 (apply list (conj query parameters)))
               (gen/tuple (s/gen ::parameterization-query)
                          (s/gen ::parameters)))))

(s/def ::query
  (s/with-gen
    (s/and vector?
           (s/+ (s/alt :property ::property
                       :prefixed-properties ::prefixed-properties
                       :aliased-property ::aliased-property
                       :parameterization ::parameterization)))
    #(gen/fmap util/combine-properties-and-groups
               (gen/vector (gen/one-of
                            [(s/gen ::property)
                             (s/gen ::prefixed-properties)
                             (s/gen ::aliased-property)
                             (s/gen ::parameterization)])
                           1 1))))

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

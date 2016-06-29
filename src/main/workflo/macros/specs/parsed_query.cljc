(ns workflo.macros.specs.parsed-query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.query]))

(s/def ::type
  #{:property :link :join})

(s/def ::name
  :workflo.macros.specs.query/property-name)

(s/def ::link-id
  :workflo.macros.specs.query/link-id)

(s/def ::join-target
  (s/with-gen
    (s/or :model :workflo.macros.specs.query/model-name
          :recursion :workflo.macros.specs.query/recursion
          :properties ::query)
    #(gen/one-of [(s/gen :workflo.macros.specs.query/model-name)
                  (s/gen :workflo.macros.specs.query/recursion)
                  (s/gen '#{[{:type :property :name name}]
                            [{:type :property :name name}
                             {:type :property :name email}]
                            [{:type :property :name name}
                             {:type :link :name current-user :link-id '_}
                             {:type :join :name friends
                              :join-target 'User}]})])))

(s/def ::parameters
  :workflo.macros.specs.query/parameters)

(defmulti  typed-property :type)

(defmethod typed-property :property [_]
  (s/keys :req-un [::type ::name]
          :opt-un [::parameters]))

(defmethod typed-property :link [_]
  (s/keys :req-un [::type ::name ::link-id]
          :opt-un [::parameters]))

(defmethod typed-property :join [_]
  (s/keys :req-un [::type ::name ::join-target]
          :opt-un [::parameters]))

(s/def ::property
  (s/multi-spec typed-property :type))

(s/def ::query
  (s/coll-of ::property :kind vector?
             :min-count 1
             :gen-max 10))

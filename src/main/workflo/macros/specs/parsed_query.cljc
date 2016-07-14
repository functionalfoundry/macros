(ns workflo.macros.specs.parsed-query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.query]))

(s/def :property/type
  #{:property})

(s/def :link/type
  #{:link})

(s/def :join/type
  #{:join})

(s/def ::name
  :workflo.macros.specs.query/property-name)

(s/def ::link-id
  :workflo.macros.specs.query/link-id)

(s/def ::join-source
  (s/with-gen
    (s/or :property ::unparameterized-property
          :link ::unparameterized-link)
    #(gen/one-of [(s/gen ::unparameterized-property)
                  (s/gen '#{{:type :link :name foo :link-id _}
                            {:type :link :name bar :link-id 5}})])))

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

(s/def ::property
  (s/keys :req-un [:property/type ::name]
          :opt-un [::parameters]))

(s/def ::unparameterized-property
  (s/keys :req-un [:property/type ::name]))

(s/def ::link
  (s/keys :req-un [:link/type ::name ::link-id]
          :opt-un [::parameters]))

(s/def ::unparameterized-link
  (s/keys :req-un [:link/type ::name ::link-id]))

(s/def ::join
  (s/keys :req-un [:join/type ::name ::join-source ::join-target]
          :opt-un [::parameters]))

(defmulti  typed-property-spec :type)

(defmethod typed-property-spec :property [_]
  ::property)

(defmethod typed-property-spec :link [_]
  ::link)

(defmethod typed-property-spec :join [_]
  ::join)

(s/def ::typed-property
  (s/multi-spec typed-property-spec :type))

(s/def ::query
  (s/with-gen
    (s/and vector? (s/+ ::typed-property))
    #(gen/vector (s/gen ::typed-property)
                 1 10)))

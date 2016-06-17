(ns workflo.macros.props
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [clojure.spec.gen :as gen]
            [clojure.test.check.generators :as tc-gen]
            [workflo.macros.props.util :as util]))

;;;; Specs for properties specifications

(s/def ::join-target-model
  (s/with-gen
    (s/and util/capitalized-symbol?
           #(not= '... %))
    #(s/gen '#{Foo Bar Baz})))

(s/def ::join-target-recursion
  (s/or :self #{'...}
        :limit (s/and integer? pos?)))

(s/def ::model-join
  (s/with-gen
    (s/and (s/map-of ::name ::join-target-model)
           #(= 1 (count %)))
    #(gen/map (s/gen ::name)
              (s/gen ::join-target-model)
              {:num-elements 1})))

(s/def ::recursive-join
  (s/with-gen
    (s/and (s/map-of ::name ::join-target-recursion)
           #(= 1 (count %)))
    #(gen/map (s/gen ::name)
              (s/gen ::join-target-recursion)
              {:num-elements 1})))

(s/def ::properties-join
  (s/with-gen
    (s/and (s/map-of ::name ::properties-spec) ;; ::properties-spec
           #(= 1 (count %)))
    #(gen/map (s/gen ::name)
              (s/gen ::properties-spec)
              {:num-elements 1})))

(s/def ::join
  (s/or :model-join ::model-join
        :recursive-join ::recursive-join
        :properties-join ::properties-join))

(s/def ::link
  (s/with-gen
    (s/and vector?
           (s/cat :name ::name
                  :id ::s/any))
    #(gen/tuple (s/gen ::name)
                (s/gen ::s/any))))

(s/def ::property
  (s/or :name symbol?
        :link ::link
        :join ::join))

(s/def ::properties
  (s/with-gen
    (s/and vector? (s/+ ::property))
    #(gen/vector (s/gen ::property))))

(s/def ::properties-group
  (s/cat :base ::name
         :children ::properties))

(s/def ::property-or-properties-group
  (s/alt :property ::property
         :properties ::properties-group))

(s/def ::properties-spec
  (s/with-gen
    (s/and vector?
           (s/+ ::property-or-properties-group))
    #(gen/fmap util/combine-properties-and-groups
               (gen/vector (tc-gen/frequency
                            [[10 (s/gen ::property)]
                             [ 1 (s/gen ::properties-group)]])
                           1 5))))

;;;; Specs for conformed properties specifications

(s/def ::conforming-property
  (s/and vector?
         (s/cat :type #{:property}
                :data (s/and vector?
                             (s/cat :type #{:name}
                                    :name symbol?)))))

(s/def ::conforming-properties-group
  (s/and vector?))

(s/def ::conforming-property-or-properties-group
  (s/alt :property ::conforming-property
         :properties ::conforming-properties-group))

;;;; Specs for parsed properties specifications

(s/def ::type
  #{:property :join :link})

(s/def ::name
  symbol?)

(s/def ::join-target
  (s/or :model util/capitalized-symbol?
        :recursion-self #{'...}
        :recursion-limit (s/and number? pos?)
        :properties ::parsed-properties))

(s/def ::link-id
  (s/or :global #(= % '_)
        :arbitrary ::s/any))

(defmulti  parsed-property-spec :type)
(defmethod parsed-property-spec :property [_]
  (s/keys :req-un [::type ::name]))
(defmethod parsed-property-spec :join [_]
  (s/keys :req-un [::type ::name ::join-target]))
(defmethod parsed-property-spec :link [_]
  (s/keys :req-un [::type ::name ::link-id]))

(s/def ::parsed-property
  (s/multi-spec parsed-property-spec :type))

(s/def ::parsed-properties
  (s/and vector? (s/+ ::parsed-property)))

;;;; Properties specification parsing

(declare parse)

(s/fdef parse-prop
  :args (s/cat :prop ::conforming-property-or-properties-group)
  :ret  ::parsed-properties)

(defn parse-prop
  "Takes a conforming prop from a properties specification and
   returns a vector of parsed properties, each in one of the following
   forms:

    {:name user/name :type :property}
    {:name user/email :type :property}
    {:name user/friends :type :join :join-target User}
    {:name user/friends :type :join
     :join-target [{:name user/name :type :property}]}
    {:name current-user :type :link :link-id _}."
  [[type data]]
  (case type
    :name       [{:name data :type :property}]
    :property   (parse-prop data)
    :properties (let [{:keys [base children]} data
                      child-properties        (->> children
                                                   (map parse-prop)
                                                   (apply concat)
                                                   (into []))]
                  (->> child-properties
                       (map (fn [child-property]
                              (update child-property :name
                                      (fn [sym]
                                        (symbol (name base)
                                                (name sym))))))
                       (into [])))
    :join       (let [[join-type join] data
                      [name target]    (first join)]
                  (case join-type
                    :model-join      [{:name name :type :join
                                       :join-target target}]
                    :recursive-join  [{:name name :type :join
                                       :join-target target}]
                    :properties-join [{:name name :type :join
                                       :join-target (parse target)}]))
    :link       (let [{:keys [name id]} data]
                  [{:name name :type :link :link-id id}])))

(s/fdef parse
  :args (s/cat :props ::properties-spec)
  :ret ::parsed-properties)

(defn parse
  "Parses a properties specification like
   [user [name email {friends User}] [current-user _]] into
   a flat collection with the following structure:

   [{:name user/name :type :property}
    {:name user/email :type :property}
    {:name user/friends :type :join :join-target User}
    {:name current-user :type :link :link-id _}].

   From this it is trivial to generate keys for destructuring
   view props and an Om Next query."
  [spec]
  (let [conforming-spec (s/conform ::properties-spec spec)
        parsed-props    (->> conforming-spec
                             (map parse-prop)
                             (apply concat)
                             (into []))]
    parsed-props))

(defn property-query
  [prop])

(defn om-query
  "Generates an Om Next query from a parsed properties specification."
  [props]
  (into [] (map property-query) props))


(defn map-keys
  "Generates keys for destructuring a map of properties from a parsed
   properties specification."
  [props]
  (into [] (map :name) props))

(s/fdef map-keys
        :args (s/cat :props :workflo.macros.props/props-spec)
        :ret vector?)

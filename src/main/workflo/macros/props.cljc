(ns workflo.macros.props
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.props.util :as util]))

;;;; Specs

(s/def ::name
  symbol?)

(s/def ::link-id
  (s/with-gen
    ::s/any
    gen/simple-type))

(s/def ::base
  ::name)

(s/def ::join-target-model
  (s/with-gen
    (s/and util/capitalized-symbol?
           #(not= '... %))
    #(s/gen '#{Foo Bar Baz})))

(s/def ::unlimited-join-recursion
  '#{...})

(s/def ::limited-join-recursion
  (s/and integer? pos?))

(s/def ::join-target-recursion
  (s/or :unlimited ::unlimited-join-recursion
        :limited ::limited-join-recursion))

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
    (s/and (s/map-of ::name ::properties-spec)
           #(= 1 (count %)))
    #(gen/map (s/gen ::name)
              (s/gen '#{[foo] [foo [bar baz]]})
              {:num-elements 1})))

(s/def ::join
  (s/or :model-join ::model-join
        :recursive-join ::recursive-join
        :properties-join ::properties-join))

(s/def ::link
  (s/with-gen
    (s/and vector?
           (s/cat :name ::name
                  :link-id ::link-id))
    #(gen/tuple (s/gen ::name)
                (s/gen ::s/any))))

(s/def ::property
  (s/or :name symbol?
        :link ::link
        :join ::join))

(s/def ::properties
  (s/with-gen
    (s/and vector? (s/+ ::property))
    #(gen/vector (s/gen ::property) 0 5)))

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
               (gen/vector (gen/one-of
                            [(s/gen ::property)
                             (s/gen ::properties-group)])
                           0 5))))

;;;; Specs for conformed properties specifications

(s/def ::conforming-name
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:name}
                  :name ::name))
    #(gen/tuple (s/gen #{:name})
                (s/gen ::name))))

(s/def ::children
  (s/with-gen
    (s/and vector?
           (s/+ (s/or :name ::conforming-name
                      :join ::conforming-join
                      :link ::conforming-link)))
    #(gen/vector (gen/one-of [(s/gen ::conforming-name)
                              (s/gen ::conforming-join)
                              (s/gen ::conforming-link)]))))

(s/def ::conforming-model-join
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:model-join}
                  :join ::model-join))
    #(gen/tuple (s/gen #{:model-join})
                (s/gen ::model-join))))

(s/def ::conforming-recursive-join
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:recursive-join}
                  :join ::recursive-join))
    #(gen/tuple (s/gen #{:recursive-join})
                (s/gen ::recursive-join))))

(s/def ::conforming-properties-join
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:properties-join}
                  :join ::properties-join))
    #(gen/tuple (s/gen #{:properties-join})
                (s/gen ::properties-join))))

(s/def ::conforming-join
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:join}
                  :data
                  (s/or :model ::conforming-model-join
                        :recursive ::conforming-recursive-join
                        :properties ::conforming-properties-join)))
    #(gen/tuple (s/gen #{:join})
                (gen/one-of [(s/gen ::conforming-model-join)
                             (s/gen ::conforming-recursive-join)
                             (s/gen ::conforming-properties-join)]))))

(s/def ::conforming-link
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:link}
                  :link (s/keys :req-un [::name ::link-id])))
    #(gen/tuple (s/gen #{:link})
                (gen/hash-map :name (s/gen ::name)
                              :link-id (s/gen ::link-id)))))

(s/def ::conforming-property
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:property}
                  :data (s/or :name ::conforming-name
                              :join ::conforming-join
                              :link ::conforming-link)))
    #(gen/tuple (s/gen #{:property})
                (gen/one-of [(s/gen ::conforming-name)
                             (s/gen ::conforming-join)
                             (s/gen ::conforming-link)]))))

(s/def ::conforming-properties-group
  (s/with-gen
    (s/and vector?
           (s/cat :type #{:properties}
                  :data (s/keys :req-un
                                [::base ::children])))
    #(gen/tuple (s/gen #{:properties})
                (gen/hash-map :base (s/gen ::base)
                              :children (s/gen ::children)))))

(s/def ::conforming-property-or-properties-group
  (s/or :property ::conforming-property
        :properties ::conforming-properties-group))

(s/def ::conforming-properties-spec
  (s/and vector?
         (s/+ ::conforming-property-or-properties-group)))

;;;; Specs for parsed properties specifications

(s/def ::type
  #{:property :join :link})

(s/def ::join-target
  (s/with-gen
    (s/or :model ::join-target-model
          :unlimited-recursion ::unlimited-join-recursion
          :limited-recursion ::limited-join-recursion
          :properties ::parsed-properties)
    #(gen/one-of [(s/gen ::join-target-model)
                  (s/gen ::unlimited-join-recursion)
                  (s/gen ::limited-join-recursion)
                  (s/gen '#{[{:type :property
                              :name foo/bar}
                             {:type :property
                              :name foo/baz}]})])))

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
  (s/with-gen
   (s/and vector? (s/+ ::parsed-property))
   #(gen/vector (s/gen ::parsed-property))))

;;;; Specs for Om Next queries

(s/def ::om-keyword-query
  keyword?)

(s/def ::om-link-query
  (s/with-gen
    (s/and vector?
           #(= 2 (count %))
           (s/cat :key ::om-keyword-query
                  :id  ::s/any))
    #(gen/tuple (s/gen ::om-keyword-query)
                (s/gen ::s/any))))

(s/def ::om-component-query
  (s/cat :call '#{om.next/get-query}
         :component symbol?))

(s/def ::om-join-query-target
  (s/with-gen
    (s/or :query ::om-query
          :recursion (s/or :unlimited ::unlimited-join-recursion
                           :limited ::limited-join-recursion)
          :component-query ::om-component-query)
    #(gen/one-of [(s/gen #{[:foo] [:foo :bar] [:foo [:bar :baz]]})
                  (s/gen ::unlimited-join-recursion)
                  (s/gen ::limited-join-recursion)
                  (s/gen ::om-component-query)])))

(s/def ::om-join-query
  (s/with-gen
    (s/and (s/map-of ::om-keyword-query
                     ::om-join-query-target)
           #(= 1 (count %)))
    #(gen/map (s/gen ::om-keyword-query)
              (s/gen ::om-join-query-target)
              {:num-elements 1})))

(s/def ::om-property-query
  (s/or :keyword ::om-keyword-query
        :link    ::om-link-query
        :join    ::om-join-query))

(s/def ::om-query
  (s/with-gen
    (s/and vector?
           (s/+ ::om-property-query))
    #(gen/vector (gen/one-of [(s/gen ::om-keyword-query)])
                 1 100)))

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
    :property   (parse-prop data)
    :name       [{:name data :type :property}]
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
    :link       (let [{:keys [name link-id]} data]
                  [{:name name :type :link :link-id link-id}])))

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

(s/fdef property-query
  :args (s/cat :prop ::parsed-property)
  :ret  ::om-property-query
  :fn   (s/or :keyword (s/and #(= :property (:type (:prop (:args %))))
                              #(s/conform ::om-keyword-query (:ret %)))
              :link    (s/and #(= :link (:type (:prop (:args %))))
                              #(s/conform ::om-link-query (:ret %)))
              :join    (s/and #(= :join (:type (:prop (:args %))))
                              #(s/conform ::om-join-query (:ret %)))))

(defn property-query
  "Generates an Om Next query for a parsed property specification."
  [prop]
  (let [kw-name (keyword (:name prop))]
    (case (:type prop)
      :property kw-name
      :link     [kw-name (:link-id prop)]
      :join     (let [target (:join-target prop)]
                  {kw-name
                   (cond
                     (or (number? target) (= '... target)) target
                     (vector? target) (into []
                                            (map property-query)
                                            target)
                     :else `(~'om.next/get-query ~target))}))))

(defn om-query
  "Generates an Om Next query from a parsed properties specification."
  [props]
  (into [] (map property-query) props))

(s/fdef map-destructuring-keys
  :args (s/cat :props ::parsed-properties)
  :ret  (s/and vector? (s/+ symbol?))
  :fn   (s/and #(= (into #{} (:ret %))
                   (into #{} (map :name) (:props (:args %))))))

(defn map-destructuring-keys
  "Generates keys for destructuring a map of properties from a parsed
   properties specification."
  [props]
  (into [] (map :name) props))

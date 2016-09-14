(ns workflo.macros.query
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.query.bind :as bind]
            [workflo.macros.specs.conforming-query :as conforming-query]
            [workflo.macros.specs.parsed-query :as parsed-query]
            [workflo.macros.specs.query :as q]))

;;;; Query parsing

(s/fdef query-type
  :args (s/cat :query (s/with-gen
                        (s/and vector?
                               (s/cat :first keyword?
                                      :rest (s/* any?)))
                        #(gen/vector (s/gen keyword?)
                                     1 10)))
  :ret keyword?
  :fn #(= (:ret %) (:first (:query (:args %)))))

(defn query-type [query]
  (first query))

(s/def ::subquery
  (s/or :simple ::conforming-query/simple
        :link ::conforming-query/link
        :join ::conforming-query/join
        :property ::conforming-query/property
        :aliased-property ::conforming-query/aliased-property
        :prefixed-properties ::conforming-query/prefixed-properties
        :parameterization ::conforming-query/parameterization))

(s/fdef parse-subquery
  :args (s/cat :query ::subquery)
  :ret ::parsed-query/query)

(defmulti parse-subquery
  "Takes a subquery and returns a vector of parsed properties, each
   in one of the following forms:

       {:name user/name :type :property :alias user-name}
       {:name user/email :type :property}
       {:name user/friends :type :join
        :join-source {name user/friends :type :property}
        :join-target User}
       {:name user/friends :type :join
        :join-source {:name user/friends :type :property}
        :join-target [{:name user/name :type :property}]}
       {:name current-user :type :link :link-id _}.

   Each of these may in addition contain an optional :parameters
   key with a {symbol ?variable}-style map and an :alias key
   with a symbol to use when destructuring instead of the
   original name."
  query-type)

(defmethod parse-subquery :simple
  [[_ name]]
  [{:name name :type :property}])

(defmethod parse-subquery :link
  [[_ [link-name link-id]]]
  [{:name link-name :type :link :link-id link-id}])

(defmethod parse-subquery :join
  [[_ join]]
  (let [[type value]    join
        [source target] (first value)
        join-source     (first (parse-subquery source))
        res             [{:name (:name join-source)
                          :type :join
                          :join-source join-source}]]
    (case type
      :properties (assoc-in res [0 :join-target]
                            (->> target
                                 (map parse-subquery)
                                 (apply concat)
                                 (into [])))
      :recursive  (assoc-in res [0 :join-target] (second target))
      :model      (assoc-in res [0 :join-target]
                            #?(:cljs (om.next/get-query (second target))
                               :clj  (second target))))))

(defmethod parse-subquery :property
  [[_ q]]
  (parse-subquery q))

(defmethod parse-subquery :aliased-property
  [[_ q]]
  (let [{:keys [property alias]} q]
    (mapv #(assoc % :alias alias) (parse-subquery property))))

(defmethod parse-subquery :prefixed-properties
  [[_ {:keys [base children]}]]
  (letfn [(prefix-name [x]
            (let [prefixed-name (symbol (str base) (str (:name x)))]
              (assoc x :name prefixed-name)))]
    (->> children
         (map parse-subquery)
         (apply concat)
         (map prefix-name)
         (into []))))

(defmethod parse-subquery :parameterization
  [[_ {:keys [query parameters]}]]
  (assoc-in (parse-subquery query) [0 :parameters] parameters))

(defmethod parse-subquery :default
  [q]
  (if ;; Workaround for extra [] around
      ;; [:aliased-property ...] in output of
      ;; conform (JIRA issue CLJ-2003)
      (and (vector? q)
           (= 1 (count q))
           (vector? (first q))
           (keyword? (ffirst q)))
    (parse-subquery (first q))
    (let [msg (str "Unknown subquery: " q)]
      (throw (new #?(:cljs js/Error :clj Exception) msg)))))

(s/fdef parse
  :args (s/cat :query ::conforming-query/query)
  :ret ::parsed-query/query)

(defn parse [query]
  (transduce (map parse-subquery)
             (comp vec concat)
             [] query))

(s/fdef conform
  :args (s/cat :query ::q/query)
  :ret ::conforming-query/query)

(defn conform [query]
  (s/conform ::q/query query))

(s/fdef conform-and-parse
  :args (s/cat :query ::q/query)
  :ret ::parsed-query/query)

(defn conform-and-parse
  "Conforms and parses a query expression like

       [user [name :as nm email {friends User}] [current-user _]]

   into a flat vector of parsed properties with the following
   structure:

       [{:name user/name :type :property :alias nm}
        {:name user/email :type :property}
        {:name user/friends :type :join :join-target User}
        {:name current-user :type :link :link-id _}].

   From this it is trivial to generate queries for arbitrary
   frameworks (e.g. Om Next) as well as keys for destructuring
   the results."
  [query]
  (parse (conform query)))

(s/def ::map-destructuring-keys
  (s/coll-of symbol? :kind vector?))

(s/fdef map-destructuring-keys
  :args (s/cat :query ::parsed-query/query)
  :ret ::map-destructuring-keys
  :fn (s/and #(= (into #{} (:ret %))
                 (into #{} (map :name) (:query (:args %))))))

(defn map-destructuring-keys
  "Generates keys for destructuring a map of query results."
  [query]
  (into [] (map :name) query))

(defn bind-query-parameters
  "Takes a parsed query and a map of named parameters and their
   values. Binds the unbound parameters in the query (that is,
   those where the value is either a symbol beginning with a ?
   or a vector of such symbols) to values of the corresponding
   parameters in the parameter map and returns the result.

   As an example, the :db/id parameter in the query

     [{:name user :type :join
       :join-target [{:name name :type :property}]
       :parameters {:db/id ?foo
                    :user/friend [?bar ?baz]}}]

   would be bound to the value 10 if the parameter map was
   {:foo 10 :bar {:baz :ruux}} and the :user/friend parameter
   would be bound to the value :ruux."
  [query params]
  (letfn [(bind-param [[k v]]
            [k (cond-> v
                 (or (bind/var? v)
                     (bind/path? v))
                 (bind/resolve params))])
          (bind-params [unbound-params]
            (into {} (map bind-param) unbound-params))
          (bind-query-params [subquery]
            (if (contains? subquery :parameters)
              (update subquery :parameters bind-params)
              subquery))
          (follow-and-bind-joins [subquery]
            (if (vector? (get subquery :join-target))
              (update subquery :join-target
                      (partial mapv bind-query-params))
              subquery))]
    (mapv (comp follow-and-bind-joins
                bind-query-params)
          query)))

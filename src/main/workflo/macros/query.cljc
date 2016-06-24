(ns workflo.macros.query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [clojure.walk :refer [keywordize-keys]]
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.conforming-query]
            [workflo.macros.specs.om-query]
            [workflo.macros.specs.parsed-query]
            [workflo.macros.specs.query]))

;;;; Properties specification parsing

(declare parse)

(s/fdef conform
  :args (s/cat :query :workflo.macros.specs.query/query)
  :ret  :workflo.macros.specs.conforming-query/query)

(defn conform
  "Validates a query and returns the parsed, conforming result."
  [query]
  (s/conform :workflo.macros.specs.query/query query))

(s/def ::subquery
  (s/or :regular-query
        :workflo.macros.specs.conforming-query/regular-query
        :parameterized-query
        :workflo.macros.specs.conforming-query/parameterized-query))

(s/fdef parse-subquery
  :args (s/cat :query ::subquery)
  :ret  :workflo.macros.specs.parsed-query/query)

(defn parse-subquery
  "Takes a subquery and returns a vector of parsed properties, each
   in one of the following forms:

       {:name user/name :type :property}
       {:name user/email :type :property}
       {:name user/friends :type :join :join-target User}
       {:name user/friends :type :join
        :join-target [{:name user/name :type :property}]}
       {:name current-user :type :link :link-id _}.

   Each of these may in addition contain an optional :parameters
   key with a {symbol ?variable}-style map."
  [[type query]]
  (case type
    :regular-query       (parse-subquery query)
    :parameterized-query (->> (:regular-query-value query)
                              (parse-subquery)
                              (mapv (fn [parsed]
                                      (assoc parsed :parameters
                                             (:parameters query)))))
    :nested-properties   (let [{:keys [base children]} query]
                           (->> children
                                (map parse-subquery)
                                (apply concat)
                                (mapv (fn [child]
                                        (update child :name
                                                (fn [sym]
                                                  (symbol
                                                   (name base)
                                                   (name sym))))))))
    :property            (parse-subquery query)
    :simple              [{:name query :type :property}]
    :link                (let [[name link-id] query]
                           [{:name name :type :link :link-id link-id}])
    :join                (parse-subquery query)
    :model-join          (let [[name target] (first query)]
                           [{:name name :type :join
                             :join-target target}])
    :recursive-join      (let [[name target] (first query)]
                           [{:name name :type :join
                             :join-target target}])
    :properties-join     (let [[name target] (first query)]
                           [{:name name :type :join
                             :join-target (parse target)}])))

(s/fdef parse
  :args (s/cat :props :workflo.macros.specs.query/query)
  :ret :workflo.macros.specs.parsed-query/query)

(defn parse
  "Parses a query expression like

       [user [name email {friends User}] [current-user _]]

   into a flat vector of parsed properties with the following
   structure:

       [{:name user/name :type :property}
        {:name user/email :type :property}
        {:name user/friends :type :join :join-target User}
        {:name current-user :type :link :link-id _}].

   From this it is trivial to generate queries for arbitrary
   frameworks (e.g. Om Next) as well as keys for destructuring
   the results."
  [query]
  (->> (conform query)
       (map parse-subquery)
       (apply concat)
       (into [])))

(s/def ::om-property-query-from-parsed-property
  (s/and
   (s/or :keyword
         (s/and #(= :property (-> % :args :prop :type))
                #(s/conform :workflo.macros.specs.om-query/keyword
                            (:ret %)))
         :link
         (s/and #(= :link (-> % :args :prop :type))
                #(s/conform :workflo.macros.specs.om-query/link
                            (:ret %)))
         :join
         (s/and #(= :join (-> % :args :prop :type))
                #(s/conform :workflo.macros.specs.om-query/join
                            (:ret %))))
   (s/or :regular
         #(not (contains? (-> % :args :prop) :parameters))
         :parameterized
         (s/and #(contains? (-> % :args :prop) :parameters)
                #(list? (:ret %))
                #(= 2 (count (:ret %)))
                #(map? (second (:ret %)))
                #(= (keywordize-keys (-> % :args :prop :parameters))
                    (second (:ret %)))))))

(s/fdef om-property-query
  :args (s/cat :prop :workflo.macros.specs.parsed-query/property)
  :ret  :workflo.macros.specs.om-query/property
  :fn   ::om-property-query-from-parsed-property)

(defn om-property-query
  "Generates an Om Next query for a parsed property specification."
  [prop]
  (let [kw-name (keyword (:name prop))
        params  (when-not (empty? (:parameters prop))
                  (->> (:parameters prop)
                       (map (fn [[k v]] [(keyword k) v]))
                       (into {})))]
    (-> (case (:type prop)
          :property kw-name
          :link     [kw-name (if (= '_ (:link-id prop))
                               ''_
                               (:link-id prop))]
          :join     (let [target (:join-target prop)]
                      {kw-name
                       (cond
                         (some #{target} #{'... ''...}) ''...
                         (number? target) target
                         (vector? target) (into []
                                                (map om-property-query)
                                                target)
                         :else `(~'om.next/get-query ~target))}))
        (cond-> params (list params)))))

(s/fdef om-query
  :args (s/cat :query :workflo.macros.specs.parsed-query/query)
  :ret  :workflo.macros.specs.om-query/query)

(defn om-query
  "Generates an Om Next query from a parsed query."
  [query]
  (into [] (map om-property-query) query))

(s/fdef map-destructuring-keys
  :args (s/cat :props :workflo.macros.specs.parsed-query/query)
  :ret  (s/and vector? (s/+ symbol?))
  :fn   (s/and #(= (into #{} (:ret %))
                   (into #{} (map :name) (:props (:args %))))))

(defn map-destructuring-keys
  "Generates keys for destructuring a map of properties from a parsed
   properties specification."
  [props]
  (into [] (map :name) props))

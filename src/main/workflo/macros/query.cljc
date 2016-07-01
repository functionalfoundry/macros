(ns workflo.macros.query
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [workflo.macros.query.util :as util]
            [workflo.macros.specs.conforming-query]
            [workflo.macros.specs.parsed-query]
            [workflo.macros.specs.query]))

;;;; Properties specification parsing

(declare conform-and-parse)
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
        :workflo.macros.specs.conforming-query/parameterized-query
        :nested-properties
        :workflo.macros.specs.conforming-query/nested-properties
        :property :workflo.macros.specs.conforming-query/property
        :simple :workflo.macros.specs.conforming-query/simple-property
        :link :workflo.macros.specs.conforming-query/link
        :join :workflo.macros.specs.conforming-query/join
        :model-join :workflo.macros.specs.conforming-query/model-join
        :recursive-join
        :workflo.macros.specs.conforming-query/recursive-join
        :properties-join
        :workflo.macros.specs.conforming-query/properties-join))

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
                             :join-target
                             #?(:cljs target
                                :clj  (second target))}])
    :properties-join     (let [[name target] (first query)]
                           [{:name name :type :join
                             :join-target
                             #?(:cljs (conform-and-parse target)
                                :clj  (parse target))}])))

(s/fdef parse
  :args (s/cat :conforming-query
               :workflo.macros.specs.conforming-query/query)
  :ret  :workflo.macros.specs.parsed-query/query)

(defn parse
  [conforming-query]
  (->> conforming-query
       (map parse-subquery)
       (apply concat)
       (into [])))

(s/fdef conform-and-parse
  :args (s/cat :props :workflo.macros.specs.query/query)
  :ret :workflo.macros.specs.parsed-query/query)

(defn conform-and-parse
  "Conforms and parses a query expression like

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
  (parse (conform query)))

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

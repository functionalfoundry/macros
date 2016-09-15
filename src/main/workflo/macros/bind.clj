(ns workflo.macros.bind
  (:require [clojure.core.specs :as core-specs]
            [clojure.test :refer [is]]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as st]
            [workflo.macros.query :as q]
            [workflo.macros.specs.bind :as sb]
            [workflo.macros.specs.parsed-query :as spq]
            [workflo.macros.specs.query :as sq]))

(s/fdef property-binding-paths
  :args (s/cat :property ::spq/typed-property
               :path (s/? (s/nilable ::sb/path)))
  :ret ::sb/paths)

(defn property-binding-paths
  "Takes a property and an optional path of parents. Returns
   a flat vector of all paths inside the property and its
   parents. Each path is again a sequence of properties,
   starting from the leaf (e.g. a regular property) and
   ending at the root of the query.

   As an example, the query [{user [user [name email]]]}]
   would result in the query AST

       [{:name user :type :join
         :join-source {:name user :type :property}
         :join-target [{:name user/name :type :property}
                       {:name user/email :type :property}]}].

   Calling property-binding-paths with the join property
   would result in two paths, one for user -> user/name and
   one for user -> user/email:

     [({:name user/name :type :property}
       {:name user :type :join :join-source ... :join-target ...})
      ({:name user/email :type :property}
       {:name user :type :join :join-source ... :join-target ...})]."
  ([property]
   (property-binding-paths property nil))
  ([property path]
   (case (:type property)
     :property [(cons property path)]
     :link     [(cons property path)]
     :join     (let [new-path (cons property path)]
                 (if (vector? (:join-target property))
                   (->> (:join-target property)
                        (map #(property-binding-paths % new-path))
                        (apply concat)
                        (into [new-path]))
                   [new-path])))))

(s/fdef binding-paths
  :args (s/cat :query (s/or :query ::sq/query
                            :parsed-query ::spq/query))
  :ret ::sb/paths)

(defn binding-paths
  "Returns a vector of all property binding paths for a query
   or parsed query."
  [query]
  (transduce (map property-binding-paths) concat []
             (cond-> query
               (s/valid? ::sq/query query) q/conform-and-parse)))

(s/fdef path-bindings
  :args (s/cat :path ::sb/path)
  :ret ::core-specs/map-bindings)

(defn path-bindings
  "Takes a property binding path and returns destructuring map
   that can be used in combination with e.g. let to pluck the
   value of the corresponding property from a query result
   and bind it to the name of the property.

   E.g. for a property path (a b/c d) (simplified notation with
   only the property names), it would return {{{a :a} :b/c} :d},
   allowing to destructure a map like {:d {:b/c {:a <val>}}}
   and bind a to <val>."
  [[leaf & path]]
  (loop [form {(or (some-> leaf :alias name symbol)
                   (some-> leaf :name name symbol))
               (keyword (:name leaf))}
         path path]
    (if (empty? path)
      form
      (recur {form (keyword (:name (first path)))}
             (rest path)))))

(s/fdef query-bindings
  :args (s/cat :query (s/or :query ::sq/query
                            :parsed-query ::spq/query))
  :ret ::core-specs/map-bindings)

(defn query-bindings
  "Takes a query or parsed query and returns map bindings to
   be applied to the corresponding query result in order to
   destructure and bind all possible properties in the query
   result to their names.

   E.g. for a query [a :as b c [d e {f [g :as h]}]] it
   would return {b :a, d :c/d, e :c/e, f :f, {{h :g} :f}}."
  [query]
  (let [paths    (binding-paths query)
        bindings (map path-bindings paths)
        merge-fn (fn [a b]
                   (if (and (map? a) (map? b))
                     (merge a b)
                     b))
        combined (apply (partial merge-with merge-fn) bindings)]
    combined))

(s/fdef with-query-bindings*
  :args (s/cat :query (s/or :query ::sq/query
                            :parsed-query ::spq/query)
               :result map?
               :body any?)
  :ret any?)

(defn with-query-bindings*
  [query result body]
  (let [bindings (query-bindings query)]
    `(let [~bindings ~result]
       ~@body)))

(defmacro with-query-bindings
  "Takes a query, a query result and an arbitrary code block.
   Wraps the code block so that all possible bindings derived
   from the query are bound to the values in the query result."
  [query result & body]
  (with-query-bindings* query result body))

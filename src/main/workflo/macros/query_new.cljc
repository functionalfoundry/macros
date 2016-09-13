(ns workflo.macros.query-new
  (:require [clojure.spec :as s]
            [workflo.macros.specs.query]))

(defn conform [query]
  (s/conform :workflo.macros.specs.query/query query))

(defn query-type [q]
  {:pre [(s/valid? (s/and vector? #(keyword? (first %))) q)]}
  (first q))

(defmulti parse-subquery query-type)

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
  (let [msg (str "Unknown subquery: " q)]
    (throw (new #?(:cljs js/Error :clj Exception) msg))))

(defn parse [query]
  (transduce (map (fn [q]
                    (println "Q" q)
                    ;; Workaround for extra [] around
                    ;; [:aliased-property ...] in output of
                    ;; conform (JIRA issue CLJ-2003)
                    (if (and (vector? q)
                             (= 1 (count q))
                             (vector? (first q))
                             (keyword? (ffirst q)))
                      (parse-subquery (first q))
                      (parse-subquery q))))
             (comp vec concat)
             [] query))

(defn conform-and-parse
  [query]
  (parse (conform query)))

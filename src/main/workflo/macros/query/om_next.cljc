(ns workflo.macros.query.om-next
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [clojure.walk :refer [keywordize-keys]]
            [workflo.macros.specs.om-query]
            [workflo.macros.specs.parsed-query]))

(s/def ::property-query-from-parsed-property
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

(s/fdef property-query
  :args (s/cat :prop :workflo.macros.specs.parsed-query/property)
  :ret  :workflo.macros.specs.om-query/property
  :fn   ::property-query-from-parsed-property)

(defn property-query
  "Generates an Om Next query for a parsed property query"
  [prop]
  (let [kw-name      (keyword (:name prop))
        params       (when-not (empty? (:parameters prop))
                       (->> (:parameters prop)
                            (map (fn [[k v]] [(keyword k) v]))
                            (into {})))
        parameterize (fn [query]
                       `(~'list ~query '~params))]
    (-> (case (:type prop)
          :property kw-name
          :link     [kw-name (if (= '_ (:link-id prop))
                               '_
                               (:link-id prop))]
          :join     (let [source (:join-source prop)
                          target (:join-target prop)]
                      {(property-query source)
                       (cond
                         (some #{target} #{'... ''...}) ''...
                         (number? target) target
                         (vector? target) (into []
                                                (map property-query)
                                                target)
                         :else `(om.next/get-query ~target))}))
        (cond-> params parameterize))))

(s/fdef query
  :args (s/cat :parsed-query :workflo.macros.specs.parsed-query/query)
  :ret  :workflo.macros.specs.om-query/query)

(defn query
  "Generates an Om Next query from a parsed query."
  [parsed-query]
  (into [] (map property-query) parsed-query))

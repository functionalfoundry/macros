(ns workflo.macros.query.om-next
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            #?(:cljs [om.next])
            #?(:cljs [workflo.macros.util.js :refer [resolve]])
            [workflo.macros.specs.om-query :as om-query]
            [workflo.macros.specs.parsed-query :as parsed-query]
            [workflo.macros.query.bind :refer [path?]]
            [workflo.macros.query.util :as util]))

(s/def ::property-query-from-parsed-property
  (s/and
   (s/or :keyword
         (s/and #(= :property (-> % :args :prop :type))
                #(s/conform ::om-query/keyword (:ret %)))
         :link
         (s/and #(= :link (-> % :args :prop :type))
                #(s/conform ::om-query/link (:ret %)))
         :join
         (s/and #(= :join (-> % :args :prop :type))
                #(s/conform ::om-query/join (:ret %))))
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
  :args (s/cat :prop ::parsed-query/property
               :gen-type #{:clj :cljs :clj->cljs})
  :ret ::om-query/property
  :fn ::property-query-from-parsed-property)

(defn property-query
  "Generates an Om Next query for a parsed property query"
  [prop gen-type]
  (let [kw-name      (keyword (:name prop))
        params       (when-not (empty? (:parameters prop))
                       (->> (:parameters prop)
                            (map (fn [[k v]]
                                   [(keyword k)
                                    (if (or (symbol? v) (path? v))
                                      `'~v v)]))
                            (into {})))
        parameterize (fn [query]
                       (case gen-type
                         :clj->cljs `(list ~query ~params)
                         :cljs `(~query ~params)
                         :clj `(~query ~params)))]
    (-> (case (:type prop)
          :property (cond-> kw-name
                      (util/backref-attr? kw-name) util/transform-backref-attr)
          :link     [kw-name (if (= '_ (:link-id prop))
                               '_
                               (:link-id prop))]
          :join     (let [source (:join-source prop)
                          target (:join-target prop)]
                      {(property-query source gen-type)
                       (cond
                         (some #{target} #{'... ''...}) ''...
                         (number? target) target
                         (vector? target)
                         (into []
                               (map #(property-query % gen-type))
                               target)
                         :else
                         #?(:cljs (om.next/get-query (cond-> target
                                                       (symbol? target)
                                                       resolve))
                            :clj `(om.next/get-query ~target)))}))
        (cond-> params parameterize))))

(s/fdef query
  :args (s/cat :parsed-query ::parsed-query/query
               :gen-type (s/? #{:clj :cljs :clj->cljs}))
  :ret ::om-query/query)

(defn query
  "Generates an Om Next query from a parsed query."
  ([parsed-query]
   (query parsed-query #?(:cljs :cljs :clj :clj)))
  ([parsed-query gen-type]
   (into [] (map #(property-query % gen-type)) parsed-query)))

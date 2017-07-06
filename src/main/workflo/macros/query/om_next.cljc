(ns workflo.macros.query.om-next
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            #?(:cljs [om.next])
            #?(:cljs [workflo.macros.util.js :refer [resolve]])
            [workflo.macros.specs.om-query :as om-query]
            [workflo.macros.specs.parsed-query :as parsed-query]
            [workflo.macros.query.bind :refer [path?]]
            [workflo.macros.query.om-util :as om-util]))


;;;; Om Next query generation


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
  :args (s/cat :prop ::parsed-query/typed-property
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
                                   [(if (vector? k)
                                      (mapv keyword k)
                                      (keyword k))
                                    (if (or (symbol? v) (path? v))
                                      `'~v v)]))
                            (into {})))
        parameterize (fn [query]
                       (case gen-type
                         :clj->cljs `(list ~query ~params)
                         :cljs `(~query ~params)
                         :clj `(~query ~params)))]
    (-> (case (:type prop)
          :property kw-name
          :link     [kw-name (if (= '_ (:link-id prop))
                               '_
                               (:link-id prop))]
          :join     (let [source (:join-source prop)
                          target (:join-target prop)]
                      {(property-query source gen-type)
                       (cond
                         (some #{target} #{'... ''...})
                         (case gen-type
                           :clj->cljs ''...
                           :cljs '...
                           :clj '...)
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


;;;; Om Next query disambiguation


(declare merge-query-exprs)
(declare disambiguate)


(defn- detect-query-merge-conflicts
  [path expr-1 expr-2]
  (cond
    ;; Parameterized vs. unparameterized
    (not= (om-util/param-expr? expr-1)
          (om-util/param-expr? expr-2))
    (throw (ex-info (str "Conflicting parameterized vs. unparameterized "
                         "queries at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))

    ;; Conflicting parameters
    (and (om-util/param-expr? expr-1)
         (om-util/param-expr? expr-2)
         (not= (om-util/param-map expr-1)
               (om-util/param-map expr-2)))
    (throw (ex-info (str "Conflicting parameters in queries at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))

    ;; Join with ... and non-...
    (and (om-util/join-expr? expr-1)
         (om-util/join-expr? expr-2)
         (or (= '... (om-util/join-target expr-1))
             (= '... (om-util/join-target expr-2)))
         (not= (om-util/join-target expr-1)
               (om-util/join-target expr-2)))
    (throw (ex-info (str "Conflicting recursive and non-recursive "
                         "join queries at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))

    ;; Join with limited recursion and no limited recursion
    (and (om-util/join-expr? expr-1)
         (om-util/join-expr? expr-2)
         (not= (number? (om-util/join-target expr-1))
               (number? (om-util/join-target expr-2))))
    (throw (ex-info (str "Conflicting join queries with limited recursion and "
                         "no limited recursion at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))

    ;; Ident vs. non-ident
    (not= (om-util/ident-expr? expr-1)
          (om-util/ident-expr? expr-2))
    (throw (ex-info (str "Conflicting ident vs. non-ident queries at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))

    ;; Different idents
    (and (om-util/ident-expr? expr-1)
         (om-util/ident-expr? expr-2)
         (not= expr-1 expr-2))
    (throw (ex-info (str "Conflicting ident queries at " path)
                    {:path path
                     :expressions [expr-1 expr-2]}))))


(defmulti ^:private merge-query-exprs
  (fn [path expr-1 expr-2]
    (detect-query-merge-conflicts path expr-1 expr-2)
    (if (= expr-1 expr-2)
      :identical
      [(om-util/expr-type expr-1)
       (om-util/expr-type expr-2)])))


(defmethod merge-query-exprs :identical
  [_ expr-1 expr-2]
  {:pre [(= expr-1 expr-2)]}
  ;; Identical expressions -> pick either
  expr-1)


(defmethod merge-query-exprs [:keyword :keyword]
  [_ expr-1 expr-2]
  {:pre [(= expr-1 expr-2)]}
  ;; Identical keywords -> pick either
  expr-1)


(defmethod merge-query-exprs [:limited-recursion :limited-recursion]
  [_ expr-1 expr-2]
  ;; Two limited recursions -> pick the one that recurses deeper
  (max expr-1 expr-2))


(defmethod merge-query-exprs [:keyword :join]
  [_ expr-1 expr-2]
  ;; Keyword vs. join -> pick the join (it returns more information)
  expr-2)


(defmethod merge-query-exprs [:join :keyword]
  [_ expr-1 expr-2]
  ;; Join vs. keyword -> pick the join (it returns more information)
  expr-1)


(defmethod merge-query-exprs [:join :join]
  [path expr-1 expr-2]
  {:pre [(= (om-util/dispatch-key expr-1)
            (om-util/dispatch-key expr-1))]}
  {(merge-query-exprs (conj path :join-source)
                      (om-util/join-source expr-1)
                      (om-util/join-source expr-2))
   (let [target-1 (om-util/join-target expr-1)
         target-2 (om-util/join-target expr-2)]
     (if (and (sequential? target-1) (sequential? target-2))
       (first (disambiguate (conj path :join-target)
                            (into [] cat [target-1 target-2])
                            {:throw-on-conflicts? true}))
       (merge-query-exprs (conj path :join-target)
                          target-1 target-2)))})

(defmethod merge-query-exprs [:param :param]
  [path expr-1 expr-2]
  {:pre [(= (om-util/dispatch-key expr-1)
            (om-util/dispatch-key expr-2))
         (= (om-util/param-map expr-1)
            (om-util/param-map expr-2))]}
  (list (merge-query-exprs (conj path :param-query)
                           (om-util/param-query expr-1)
                           (om-util/param-query expr-2))
        (om-util/param-map expr-1)))


(defn- try-merge-query-exprs
  [path expr-1 expr-2 throw-on-conflicts?]
  (if throw-on-conflicts?
    (merge-query-exprs path expr-1 expr-2)
    (try
      (merge-query-exprs path expr-1 expr-2)
      (catch #?(:clj Exception :cljs js/Error) error
        nil))))


(defn- disambiguate-query-expr
  [path expr]
  (case (om-util/expr-type expr)
    :join  {(om-util/join-source expr)
            (let [join-target (om-util/join-target expr)]
              (if (sequential? join-target)
                (first (disambiguate (conj path :join-target)
                                     (om-util/join-target expr)
                                     {:throw-on-conflicts? true}))
                join-target))}
    :param (list (disambiguate-query-expr (conj path :param-query)
                                    (om-util/param-query expr))
                 (om-util/param-map expr))
    expr))


(defn- merge-or-append-query-expr
  [path exprs expr {:keys [throw-on-conflicts?]
                    :or   {throw-on-conflicts? false}
                    :as   opts}]
  (loop [exprs-out []
         exprs-to-try exprs]
    (if (empty? exprs-to-try)
      (conj exprs-out expr)
      (if-some [merged-expr (try-merge-query-exprs path
                                                   (first exprs-to-try) expr
                                                   throw-on-conflicts?)]
        (into exprs-out cat [[merged-expr] (rest exprs-to-try)])
        (recur (conj exprs-out (first exprs-to-try))
               (rest exprs-to-try))))))


(defn- disambiguate-query-exprs
  [path exprs opts]
  (letfn [(disambiguate-query-expr-at-index [index expr]
            (disambiguate-query-expr (conj path index) expr))
          (merge-or-append-query-expr* [exprs-out expr]
            (merge-or-append-query-expr path exprs-out expr opts))]
    (->> exprs
         (map-indexed disambiguate-query-expr-at-index)
         (reduce merge-or-append-query-expr* []))))


(defn- spread-query-exprs-into-query-seq
  [query-seq exprs]
  (reduce (fn [query-seq' [index expr]]
            (update query-seq' index (comp vec conj) expr))
          query-seq
          (map-indexed vector exprs)))


(defn disambiguate
  "Disambiguates a query and returns a sequence of non-conflicting
   queries that together correspond ask for the same information
   as the original query."
  ([query]
   (disambiguate [] query {}))
  ([query opts]
   (disambiguate [] query opts))
  ([path query opts]
   (reduce (fn [query-seq [key exprs]]
             (->> (disambiguate-query-exprs (conj path key) exprs opts)
                  (spread-query-exprs-into-query-seq query-seq)))
           [] (group-by om-util/dispatch-key query))))

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


(defn merge-join-exprs
  "Merges two join expressions by merging the join
   source expressions and disambiguating the queries
   of both joins."
  [path expr-1 expr-2]
  (println path "MERGE JOIN EXPRS")
  (println path "-" expr-1)
  (println path "-" expr-2)
  (let [join-source (merge-query-exprs (conj path :join-source)
                                       [(om-util/join-source expr-1)
                                        (om-util/join-source expr-2)])
        join-target (-> (disambiguate (conj path :join-target)
                                      (into [] cat
                                            [(om-util/join-target expr-1)
                                             (om-util/join-target expr-2)])
                                      {:handle-conflicts? false})
                        (first))]
    (println path "-> JOIN SOURCE" join-source)
    (println path "-> JOIN TARGET" join-target)
    {join-source join-target}))


(defn merge-param-exprs
  "Merges two parameterized expressions by merging the
   queries and picking either of the parameter maps,
   assuming their are identical."
  [path expr-1 expr-2]
  (list (merge-query-exprs (conj path :param-query)
                           [(om-util/param-query expr-1)
                            (om-util/param-query expr-2)])
        (om-util/param-map expr-1)))


(defn merge-query-exprs
  "Recursively merges query expressions that correspond to the same
   dispatch key. Throws an exception if there are two conflicting
   expressions anywhere inside the top-level expressions."
  [path exprs]
  (reduce (fn [expr-1 expr-2]
            (println path "MERGE")
            (println path expr-1 (cond
                                   (om-util/param-expr? expr-1) :param
                                   (om-util/join-expr? expr-1) :join
                                   (om-util/ident-expr? expr-1) :ident
                                   (keyword? expr-1) :keyword))
            (println path expr-2 (cond
                                   (om-util/param-expr? expr-2) :param
                                   (om-util/join-expr? expr-2) :join
                                   (om-util/ident-expr? expr-2) :ident
                                   (keyword? expr-2) :keyword))
            (cond
              ;; Identical expressions -> pick either
              (= expr-1 expr-2) expr-1

              ;; Parameterized vs. unparamterized -> conflict
              (not= (om-util/param-expr? expr-1)
                    (om-util/param-expr? expr-2))
              (throw (ex-info (str "Conflicting parameterized and unparameterized queries at " path)
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

              ;; Ident vs. non-ident -> conflict
              (not= (om-util/ident-expr? expr-1)
                    (om-util/ident-expr? expr-2))
              (throw (ex-info (str "Conflicting ident vs. non-ident queries at " path)
                              {:path path
                               :expressions [expr-1 expr-2]}))

              ;; Different idents -> conflict
              (and (om-util/ident-expr? expr-1)
                   (om-util/ident-expr? expr-2)
                   (not= expr-1 expr-2))
              (throw (ex-info (str "Conflicting ident queries at " path)
                              {:path path
                               :expressions [expr-1 expr-2]}))

              (keyword? expr-1)
              (cond
                ;; Keyword vs. join -> pick the join (it returns more information)
                (om-util/join-expr? expr-2) expr-2

                ;; Keyword vs. keyword -> pick either
                :else expr-1)

              (om-util/join-expr? expr-1)
              (cond
                ;; Join vs. keyword -> pick the join (it returns more information)
                (keyword? expr-2) expr-1

                ;; Join vs. join -> merge
                :else (merge-join-exprs path expr-1 expr-2))

              (om-util/param-expr? expr-1)
              (merge-param-exprs path expr-1 expr-2)))
          (first exprs)
          (rest exprs)))


(defn try-merge-query-exprs
  [path exprs handle-conflicts?]
  (try
    (println path "TRY MERGE QUERY EXPRS:")
    (doseq [expr exprs]
      (println path "-" expr))
    (let [res (merge-query-exprs path exprs)]
      (println path ">" res)
      res)
    (catch #?(:clj Exception :cljs js/Error) error
      (if-not handle-conflicts?
        (throw error)
        nil))))


(defn disambiguate-expr
  [path expr]
  (println path "DISAMBIGUATE EXPR:" expr)
  (cond
    (keyword? expr) expr

    (om-util/ident-expr? expr) expr

    (om-util/join-expr? expr)
    {(om-util/join-source expr)
     (-> (disambiguate (conj path :join-target)
                       (om-util/join-target expr)
                       {:handle-conflicts? false})
         (first))}

    (om-util/param-expr? expr)
    (list (disambiguate-expr (conj path :param-query)
                             (om-util/param-query expr))
          (om-util/param-map expr))))


(defn disambiguate-key-exprs
  [path exprs {:keys [handle-conflicts?] :or {handle-conflicts? true}}]
  (println path "DISAMBIGUATE KEY EXPRS:")
  (doseq [expr exprs]
    (println path "-" expr))
  (let [unambiguous-exprs (map-indexed (fn [index expr]
                                         (disambiguate-expr (conj path index) expr))
                                       exprs)
        _   (println path "UNAMBIGUOUS KEY EXPRS:")
        _   (doseq [expr unambiguous-exprs]
              (println path "-" expr))
        res (reduce (fn [exprs' expr]
                      (loop [exprs-out    []
                             exprs-to-try exprs']
                        (println path "EXPRS OUT" exprs-out)
                        (println path "EXPRS TO TRY" exprs-to-try)
                        (if (empty? exprs-to-try)
                          (conj exprs-out expr)
                          (let [target-expr (first exprs-to-try)
                                merged-expr (try-merge-query-exprs path
                                                                   [target-expr expr]
                                                                   handle-conflicts?)]
                            (if merged-expr
                              (concat exprs-out
                                      (cons merged-expr
                                            (rest exprs-to-try)))
                              (recur (conj exprs-out target-expr)
                                     (rest exprs-to-try)))))))
                    [(first unambiguous-exprs)]
                    (rest unambiguous-exprs))]
    (println path ">" res)
    res))


(defn spread-exprs-into-query-seq
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
             (->> (disambiguate-key-exprs (conj path key) exprs opts)
                  (spread-exprs-into-query-seq query-seq)))
           []
           (group-by om-util/dispatch-key query))))

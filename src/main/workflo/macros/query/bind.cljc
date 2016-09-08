(ns workflo.macros.query.bind
  (:refer-clojure :exclude [resolve var?])
  (:require [clojure.spec :as s]
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [workflo.macros.specs.parsed-query]))

(s/def ::var
  (s/with-gen
    (s/and symbol?
           #(= \? (first (str %))))
    #(gen/fmap (fn [sym]
                 (symbol (str "?" (str sym))))
               (s/gen symbol?))))

(s/fdef var?
  :args (s/cat :x any?)
  :ret  boolean?
  :fn   #(= (:ret %) (s/valid? ::var (-> % :args :x))))

(defn var?
  [x]
  (and (symbol? x)
       (= \? (first (str x)))))

(s/def ::path
  (s/coll-of ::var :kind vector? :min-count 0 :gen-max 10))

(s/fdef path?
  :args (s/cat :x any?)
  :ret  boolean?
  :fn   #(= (:ret %) (s/valid? ::path (-> % :args :x))))

(defn path?
  [x]
  (and (vector? x)
       (every? var? x)))

(s/fdef denamespace
  :args (s/cat :x any?)
  :ret  any?
  :fn   (s/or :unnamed #(= (-> % :ret) (-> % :args :x))
              :symbol  (s/and #(symbol? (-> % :args :x))
                              #(= (-> % :ret)
                                  (symbol (name (-> % :args :x)))))
              :keyword (s/and #(keyword? (-> % :args :x))
                              #(= (-> % :ret)
                                  (keyword (name (-> % :args :x)))))))

(defn denamespace
  [x]
  (cond-> x
    (keyword? x) ((comp keyword name))
    (symbol? x)  ((comp symbol name))))

(s/fdef denamespace-keys
  :args (s/cat :m (s/map-of any? any?))
  :ret  (s/map-of any? any?)
  :fn   (s/and #(= (set (keys (-> % :ret)))
                   (set (map denamespace
                             (keys (-> % :args :m)))))
               (fn [{:keys [args ret]}]
                 (every? (fn [[k v]]
                           (= v (ret (denamespace k))))
                         (:m args)))))

(defn denamespace-keys
  [m]
  (if (map? m)
    (zipmap (map denamespace (keys m))
            (vals m))
    m))

(s/fdef resolve-var
  :args (s/cat :var ::var :m any?)
  :ret  any?)

(defn resolve-var
  [var m]
  (let [vname (subs (str var) 1)
        kw    (keyword vname)]
    (or (get m kw)
        (when (and (nil? (namespace kw))
                   (map? m))
          (get (denamespace-keys m) kw)))))

(s/fdef resolve-path
  :args (s/cat :path ::path
               :m any?)
  :ret  any?)

(defn resolve-path [path m]
  (loop [path path m m]
    (let [[var & remainder] path
          val (when (var? var)
                (resolve-var var m))]
      (cond->> val
        (and val (not (empty? remainder)))
        (recur remainder)))))

(s/fdef resolve
  :args (s/cat :var-or-path (s/or :var ::var
                                  :path ::path)
               :m any?)
  :ret  any?)

(defn resolve
  [var-or-path m]
  (cond
    (var? var-or-path)  (resolve-var var-or-path m)
    (path? var-or-path) (resolve-path var-or-path m)
    :else               nil))

(ns workflo.macros.bind
  (:require [clojure.test :refer [is]]
            [clojure.spec :as s]
            [clojure.spec.test :as st]
            [workflo.macros.query :as q]
            [workflo.macros.specs.query :as sq]))

(defn property-binding-paths
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

(defn query-binding-paths
  [query]
  (transduce (map property-binding-paths) concat []
             (q/conform-and-parse query)))

(defn path-destructuring-form
  [[leaf & path]]
  (loop [form {(or (:alias leaf)
                   (symbol (name (:name leaf))))
               (keyword (:name leaf))}
         path path]
    (if (empty? path)
      form
      (recur {form (keyword (:name (first path)))}
             (rest path)))))

(defn query-bindings
  "Takes a query and returns a deep destructuring form to
   be applied to the corresponding query result."
  [query]
  (let [paths    (query-binding-paths query)
        bindings (map path-destructuring-form paths)
        combined (apply (partial merge-with merge) bindings)]
    (println "QUERY BINDINGS: PATHS")
    (clojure.pprint/pprint paths)
    (println "QUERY BINDINGS: BINDINGS")
    (clojure.pprint/pprint bindings)
    (println "QUERY BINDINGS: COMBINED")
    (clojure.pprint/pprint combined)
    combined))

(s/fdef with-query-bindings*
  :args (s/cat :query ::sq/query
               :result any?
               :body (s/* any?)))

(defn with-query-bindings*
  [query result body]
  (let [bindings (query-bindings query)]
    `(let [~bindings ~result]
       ~@body)))

(defmacro with-query-bindings
  [query result & body]
  (with-query-bindings* query result body))

(st/instrument `with-query-bindings*)

;; Regular properties

(with-query-bindings [a b]
  {:a :aval :b :bval}
  (is (= [a b] [:aval :bval])))

;; Links

(with-query-bindings [[a _] [b 1] [c :x]]
  {:a :aval :b :bval :c :cval}
  (is (= [a b c] [:aval :bval :cval])))

;; Joins with property sources

(with-query-bindings [{a [b c]} {d ...} {e 5}]
  {:a {:b :bval :c :cval} :d :dval :e :eval}
  (is (= [a b c d e] [{:b :bval :c :cval}
                      :bval :cval :dval :eval])))

;; Joins with link sources

(with-query-bindings [{[a _] [b c]} {[d _] ...}]
  {:a {:b :bval :c :cval} :d :dval}
  (is (= [a b c d] [{:b :bval :c :cval} :bval :cval :dval])))

;; Prefixed properties

(with-query-bindings [a [b c] d [e f]]
  {:a/b :abval :a/c :acval :d/e :deval :d/f :dfval}
  (is (= [b c e f] [:abval :acval :deval :dfval])))

;; Aliased regular properties

(with-query-bindings [a :as b b :as c]
  {:a :aval :b :bval}
  (is (= [b c] [:aval :bval])))

;; Aliased links

(with-query-bindings [[a _] :as b [b _] :as c]
  {:a :aval :b :bval}
  (is (= [b c] [:aval :bval])))

;; Aliased joins

(with-query-bindings [{a [b]} :as c {b [d]} :as e]
  {:a {:b :abval} :b {:d :bdval}}
  (is (= [c b e d] [{:b :abval} :abval
                    {:d :bdval} :bdval])))

;; Aliased prefixed properties

(with-query-bindings [a [b :as c c :as d]]
  {:a/b :abval :a/c :acval}
  (is (= [c d] [:abval :acval])))

;; Parameterizations

(with-query-bindings [(a {b c d e}) (b {f g})]
  {:a :aval :b :bval}
  (is (= [a b] [:aval :bval])))

;; Joins with sub-joins

(with-query-bindings [{a [b [c]
                          d [e]
                          {f [g
                              {h [i]}]}]}]
  {:a {:b/c :abcval
       :d/e :adeval
       :f {:g :afgval
           :h {:i :afhival}}}}
  (is (= [a c e f g h i]
         [{:b/c :abcval
           :d/e :adeval
           :f {:g :afgval
               :h {:i :afhival}}}
          :abcval
          :adeval
          {:g :afgval
           :h {:i :afhival}}
          :afgval
          {:i :afhival}
          :afhival])))

;; Joins with sub-links

(with-query-bindings [{a [b [c] [d _]]}]
  {:a {:b/c :abcval :d :adval}}
  (is (= [a c d] [{:b/c :abcval :d :adval} :abcval :adval])))

;; Joins with sub-aliases

(with-query-bindings [{[a _] [b [c :as d] d :as e]}]
  {:a {:b/c :abcval :d :adval}}
  (is (= [a d e] [{:b/c :abcval :d :adval} :abcval :adval])))

;; Deeply nested queries with joins and aliases

(with-query-bindings [{a [b {c [d :as e]}]}]
  {:a {:b :abval :c {:d :acdval}}}
  (is (= [a b c e]
         [{:b :abval :c {:d :acdval}}
          :abval
          {:d :acdval}
          :acdval])))

;;;; Idea for collections

;; Collection query

(with-query-bindings [{users [db [id] user [name email]]}]
  {:users [{:db/id 1 :user/name "Joe" :user/email "joe@joe.org"}
           {:db/id 2 :user/name "Jeff" :user/email "jeff@email.org"}]}
  users)

;; Current way to bind each user's name etc.

;; NOTE: This requires repeating [db [id] user [name email]]

(with-query-bindings [{users [db [id] user [name email]]}]
  {:users [{:db/id 1 :user/name "Joe" :user/email "joe@joe.org"}
           {:db/id 2 :user/name "Jeff" :user/email "jeff@email.org"}]}
  (for [user users]
    (with-query-bindings [db [id] user [name email]] user
      [id name email])))

;; Better way, perhaps, if possible

;; NOTE: This would store the query, the paths, bindings etc.
;; in a local var inside (with-query-bindings ...); with-path-bindings
;; would then check if that var is defined and, if so, perform
;; destructuring and
;; binding according to the [users] path, which it resolves to
;; [db [id] user [name email]]

(with-query-bindings [{users [db [id] user [name email]]}]
  {:users [{:db/id 1 :user/name "Joe" :user/email "joe@joe.org"}
           {:db/id 2 :user/name "Jeff" :user/email "jeff@email.org"}]}
  (for [user users]
    (with-path-bindings [users] user
      [id name email])))

;; Alternative idea: Let with-query-bindings transform its body,
;; search for e.g. workflo.macros.bind/for expressions, analyze their
;; binding vector and relate it to the query for destructuring:

(with-query-bindings [{users [db [id] user [name email]]}]
  {:users [{:db/id 1 :user/name "Joe" :user/email "joe@joe.org"}
           {:db/id 2 :user/name "Jeff" :user/email "jeff@email.org"}]}
  (b/for [user users]
    [id name email]))

;; NOTE: How would filtering or transforming the users above work?

;; NOTE: We may want a good way to bind attributes of e.g.
;; a particular user (e.g. the first) with a terse syntax.

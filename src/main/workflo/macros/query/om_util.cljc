(ns workflo.macros.query.om-util)


(defn ident-expr?
  "Returns true if expr is an ident expression."
  [expr]
  (and (vector? expr)
       (= (count expr) 2)
       (keyword? (first expr))))


(defn join-expr?
  "Returns true if expr is a join expression."
  [expr]
  (and (map? expr)
       (= (count expr) 1)))


(defn param-expr?
  "Returns true if q is a parameterized query expression."
  [expr]
  (or (and (seq? expr)
           (= (count expr) 2))
      (and (seq? expr)
           (= (count expr) 3)
           (= 'clojure.core/list (first expr)))))


(def join-source
  "Returns the source of a join query expression."
  ffirst)


(def join-target
  "Return the target of a join query expression."
  (comp second first))


(defn param-query
  "Returns the query of a parameterized query expression."
  [expr]
  (if (= (count expr) 3)
    (second expr)
    (first expr)))


(def param-map
  "Returns the parameter map of a parameterized query expression."
  last)


(def ident-name
  "Returns the name of an ident expression."
  first)


(defn dispatch-key
  "Returns the key the results for a query expression will be
   stored under in the query result map. E.g. the query
   `{:foo [:bar :baz]}` would store its results under `:foo`,
   whereas the query `{:bar [:foo]}` would store its results
   under `:bar`."
  [expr]
  (cond
    (keyword? expr) expr
    (number? expr) nil
    (ident-expr? expr) (dispatch-key (ident-name expr))
    (join-expr? expr) (dispatch-key (join-source expr))
    (param-expr? expr) (dispatch-key (param-query expr))
    :else (throw (ex-info "Invalid query expression passed to `dispatch-key`"
                          {:expression expr}))))


(defn expr-type
  "Returns the type of the expression as a keyword (e.g.
   :keyword, :ident, :join, :param)."
  [expr]
  (cond
    (keyword? expr) :keyword
    (number? expr) :limited-recursion
    (= '... expr) :unlimited-recursion
    (ident-expr? expr) :ident
    (join-expr? expr) :join
    (param-expr? expr) :param))

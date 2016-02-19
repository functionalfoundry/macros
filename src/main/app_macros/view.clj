(ns app-macros.view
  (:require [app-macros.util.string :refer [camel->kebab]]))

;;;; Property specifications

(defn pad-by
  "Add pad in between any two consecutive values in coll for which
   pred returns the same result. As an example, assume the following
   use:

       (pad-by type :same-type [:foo :bar [1 2] {3 4} {5 6}])

   The result would be

       [:foo :same-type :bar [1 2] {3 4} :same-type {5 6}].

   If called without coll, returns a transducer."
  ([pred pad]
   (fn [rf]
     (let [pv (volatile! nil)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [prior @pv]
            (vreset! pv input)
            (if (pred prior input)
              (rf (rf result pad) input)
              (rf result input))))))))
  ([pred pad coll] (sequence (pad-by pred pad) coll)))

(defn parse-props-spec
  "Parse a props spec like [user [name email {friends User}]] into
   a flat collection with the following structure:

   [{:name user/name :join nil}
    {:name user/email :join nil}
    {:name user/friends :join User}].

   From this it is trivial to a) generate keys for destructuring
   view props and b) generate an Om Next query."
  [spec]
  (letfn [(prop-type [p]
            (cond
              (or (symbol? p) (map? p)) :prop
              (vector? p)               :children
              :else                     :unknown))
          (prop-name [p]
            (cond
              (symbol? p) (name p)
              (map? p)    (name (ffirst p))
              :else       nil))
          (parse-prop [parent p]
            (cond
              (symbol? p) {:name (symbol (prop-name parent)
                                         (prop-name p))
                           :join nil}
              (map? p)    {:name (symbol (prop-name parent)
                                         (prop-name p))
                           :join (second (first p))}
              :else       nil))
          (parse-step [result [p children]]
            {:pre [(= (prop-type p) :prop)
                   (or (nil? children)
                       (= (prop-type children) :children))]}
            (concat result
                    (cond
                      (nil? children)    [(parse-prop nil p)]
                      (vector? children) (mapv #(parse-prop p %)
                                               children))))]
    (->> (pad-by #(= (prop-type %1) (prop-type %2)) nil spec)
         (partition-all 2 2)
         (reduce parse-step []))))

;;;; Om Next query generation

(defn generate-query-fn
  "Generate a (query ...) function from the props spec."
  [props]
  (letfn [(generate-read-key [ret {:keys [name join]}]
            (conj ret
                  (cond
                    join  (hash-map (keyword name)
                                    `(~'om/get-query ~join))
                    :else (keyword name))))]
    (list 'query (reduce generate-read-key [] props))))

(def fn-specs
  "Function specifications for all functions that are not Object
   instance functions taking [this] as the sole argument."
  '{query-params [:static   (static om.next/IQueryParams) [this]]
    query        [:static   (static om.next/IQuery)       [this]]
    ident        [:instance (static om.next/Ident)        [this props]]
    keyfn        [:factory  nil                           [props]]
    validator    [:factory  nil                           [props]]})

(def fn-aliases
  "A few aliases allowing to define view functions with shorter or
   more logicial names."
  '{key      keyfn
    validate validator})

(defn fn-scope
  "Return the scope (:static, :instance, :props) for a view function."
  [[name & args-and-body]]
  (let [[scope _ _] (fn-specs name)]
    (or scope :instance)))

(defn fn-protocol
  "Return the scope (e.g. (static om.next/IQuery) for a view function."
  [[name & args-and-body]]
  (let [[_ protocol _ :as fn-spec] (fn-specs name)]
    (if fn-spec protocol '(Object))))

(defn fn-args
  "Return the arguments (e.g. [this]) for a view function."
  [[name & args-and-body]]
  (let [[_ _ args] (fn-specs name)]
    (or args '[this])))

(defn fn-alias
  "Return the alias for a view function or the name of the
   function itself if no alias is defined."
  [[name & _]]
  (or (fn-aliases name) name))

(defn resolve-fn-alias
  "Resolve the function alias for a view function."
  [[name & body :as f]]
  (let [alias (fn-alias f)]
    `(~alias ~@body)))

(defn maybe-generate-query-fn
  "If (query ...) is missing from fns, it is generated automatically
   from the props spec."
  [fns props]
  (cond-> fns
    (not (some '#{query} (map first fns)))
    (conj (generate-query-fn props))))

(defn inject-fn-args
  "Injects a function argument binding vector into f if it doesn't
   already have one."
  [[name & body :as f]]
  (let [args (fn-args f)]
    (if args
      `(~name ~args ~@body)
      `(~name [~'this] ~@body))))

(defn inject-props
  "Wrap the body of a function of the form (name [args] body)
   in a let that destructures props and computed props
   according to the destructuring symbols in props and
   computed."
  [[name args & body :as f] props computed]
  (let [scope (fn-scope f)]
    (if (not= scope :static)
      (let [prop-names      (map :name props)
            computed-names  (map :name computed)
            this-index      (.indexOf args 'this)
            props-index     (.indexOf args 'props)
            actual-props    (if (>= props-index 0)
                              (args props-index)
                              (if (not= scope :static)
                                `(~'om/props ~(args this-index))
                                nil))
            actual-computed `(~'om/get-computed ~actual-props)]
        `(~name ~args
          (~'let [{:keys [~@prop-names]} ~actual-props
                  {:keys [~@computed-names]} ~actual-computed]
           ~@body)))
      f)))

(defn anonymous-fn
  "Make a function f anonymous by replacing its name with fn."
  [f]
  (cons 'fn (rest f)))

(defn defview*
  ([name forms]
   (defview* name forms nil))
  ([name forms env]
   (let [prop-specs         (take-while vector? forms)
         props              (parse-props-spec (first prop-specs))
         computed           (parse-props-spec (second prop-specs))
         fns                (drop-while vector? forms)
         fns-aliased        (map resolve-fn-alias fns)
         fns-with-query     (maybe-generate-query-fn fns-aliased props)
         fns-with-args      (map inject-fn-args fns-with-query)
         fns-with-props     (map #(inject-props % props computed)
                                 fns-with-args)
         factory-fns        (filter #(= (fn-scope %) :factory)
                                    fns-with-props)
         factory-params     (zipmap (map (comp keyword first)
                                         factory-fns)
                                    (map #(anonymous-fn %)
                                         factory-fns))
         view-fns           (->> fns-with-props
                                 (remove #(some #{%} factory-fns))
                                 (group-by fn-protocol))
         flat-view-fns      (apply concat (interleave (keys view-fns)
                                                      (vals view-fns)))]
     `(do
        (om.next/defui ~name
          ~@flat-view-fns)
        (def ~(symbol (camel->kebab (str name)))
          (om.next/factory ~name ~factory-params))))))

(defmacro defview
  "Create a new view with the given name.

   Takes an optional properties spec, an optional computed properties
   spec and an arbitrary number of Om Next component functions (such
   as ident, query, query-params, initLocalState or render) and
   JavaScript object methods, without requiring their protocols or
   argument bindings (like [this] or [props]) to be included in the
   definition.

   Based on the properties and computed properties spec, defview
   will wrap the function bodies of all instance functions (ident,
   render, lifecycle functions, any object methods) in a destructuring
   let that makes the specified properties available inside these
   functions.

   Usage:

      (defview User
        [name email address [street city zipcode]]
        [clicked-fn]
        (key name)
        (validate ...)
        (ident [:user/by-name name])
        (render
          (html
            [:div.user {:onClick clicked-fn}
             [:h2 name \"(\" email \")\"]
             [:ul.address
              [:li street]
              [:li city]
              [:li zipcode]]])))

   The above example would define the following:

     * An Om Next component called User
     * A component factory called user, with a :keyfn, derived from
       (key ...), and a :validator, derived from (validate ...)."
  [name & forms]
  (defview* name forms &env))

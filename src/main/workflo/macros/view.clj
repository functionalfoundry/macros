(ns workflo.macros.view
  (:require [clojure.string :as str]
            [workflo.macros.props :as p]
            [workflo.macros.util.string :refer [camel->kebab]]))

;;;; Om Next query generation

(defn generate-query-fn
  "Generate a (query ...) function from the props spec."
  [props]
  (list 'query (p/om-query props)))

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

(defn raw-fn?
  "Returns true if f is a raw function, that is if its name
   starts with a ., indicating that its function signature
   should be left alone."
 [[name & body :as f]]
 (str/starts-with? (str name) "."))

(defn inject-fn-args
  "Inject a function argument binding vector into f if it doesn't
   already have one."
  [[name & body :as f]]
  (let [args (fn-args f)]
    (if args
      `(~name ~args ~@body)
      `(~name [~'this] ~@body))))

(defn maybe-inject-fn-args
  "Inject a function argument binding vector into f unless it
   is a raw function that is assumed to define its own arguments."
  [[name & body :as f]]
  (cond-> f
    (not (raw-fn? f))
    inject-fn-args))

(defn normalize-fn-name
  "Normalize the function name of of. This removes the leading .
   from the names of raw functions."
  [[name args & body :as f]]
  (let [name (symbol (cond-> (str name) (raw-fn? f) (subs 1)))]
    `(~name ~args ~@body)))

(defn inject-props
  "Wrap the body of a function of the form (name [args] body)
   in a let that destructures props and computed props
   according to the destructuring symbols in props and
   computed."
  [[name args & body :as f] props computed]
  (let [scope (fn-scope f)]
    (if (not= scope :static)
      (let [prop-keys       (p/map-keys props)
            computed-keys   (p/map-keys computed)
            this-index      (.indexOf args 'this)
            props-index     (.indexOf args 'props)
            actual-props    (if (>= props-index 0)
                              (args props-index)
                              (if (not= scope :static)
                                `(~'om/props ~(args this-index))
                                nil))
            actual-computed `(~'om/get-computed ~actual-props)]
        `(~name ~args
          (~'let [{:keys [~@prop-keys]} ~actual-props
                  {:keys [~@computed-keys]} ~actual-computed]
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
         props              (p/parse (first prop-specs))
         computed           (p/parse (second prop-specs))
         fns                (drop-while vector? forms)
         fns-aliased        (map resolve-fn-alias fns)
         fns-with-query     (maybe-generate-query-fn fns-aliased props)
         fns-with-args      (map maybe-inject-fn-args fns-with-query)
         fns-with-names     (map normalize-fn-name fns-with-args)
         fns-with-props     (map #(inject-props % props computed)
                                 fns-with-names)
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

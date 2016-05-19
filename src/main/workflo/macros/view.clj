(ns workflo.macros.view
  (:require [clojure.string :as str]
            [workflo.macros.props :as p]
            [workflo.macros.util.string :refer [camel->kebab]]))

;;;; Om Next query generation

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

(defn generate-ident-fn
  "Generate a (ident ...) function from the props spec."
  [props]
  (list 'ident '[:db/id id]))

(defn generate-key-fn
  "Generate a (key ...) function from the props spec."
  [props]
  (list 'key 'id))

(defn generate-query-fn
  "Generate a (query ...) function from the props spec."
  [props]
  (list 'query (p/om-query props)))

(defn maybe-generate-ident-fn
  "If the props spec includes a :db/id property, an (ident ...)
   function is automatically generated based on this ID."
  [props fns]
  (cond-> fns
    (and (some #{'db/id} (map :name props))
         (not (some #{'ident} (map first fns))))
    (conj (generate-ident-fn props))))

(defn maybe-generate-key-fn
  "If the props spec includes a :db/id property, a (key ...)
   function is automatically generated based on this ID."
  [props fns]
  (cond-> fns
    (and (some #{'db/id} (map :name props))
         (not (some #{'keyfn 'key} (map first fns))))
    (conj (generate-key-fn props))))

(defn maybe-generate-query-fn
  "If (query ...) is missing from fns, it is generated automatically
   from the props spec."
  [props fns]
  (cond-> fns
    (and (not (empty? props))
         (not (some #{'query} (map first fns))))
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
  "Normalize the function name of f. This removes the leading .
   from the names of raw functions."
  [[name args & body :as f]]
  (let [name (symbol (cond-> (str name) (raw-fn? f) (subs 1)))]
    `(~name ~args ~@body)))

(defn wrap-render
  "Wraps the body of the function f in a wrapper view according to
   the :wrapper-view option set via workflo.macros.view/configure!."
  [[name args & body :as f]]
  `(~name ~args ((workflo.macros.view/wrapper)
                  (~'om.next/props ~'this)
                  ~@body)))

(defn maybe-wrap-render
  "If f is the render function, checks whether it has more than one
   child expression. If so, wraps these children in a wrapper view
   according to the :wrapper-view option set via
   workflo.macros.view/configure!."
  [[name args & body :as f]]
  (cond-> f
    (and (= name 'render)
         (> (count body) 1))
    wrap-render))

(defn inject-props
  "Wrap the body of a function of the form (name [args] body)
   in a let that destructures props and computed props
   according to the destructuring symbols in props and
   computed."
  [[name args & body :as f] props computed]
  (let [scope (fn-scope f)]
    (if (not= scope :static)
      (let [prop-keys         (p/map-keys props)
            computed-keys     (p/map-keys computed)
            this-index        (.indexOf args 'this)
            props-index       (.indexOf args 'props)
            actual-props      (if (>= props-index 0)
                                (args props-index)
                                (if (not= scope :static)
                                  `(~'om/props ~(args this-index))
                                  nil))
            actual-computed   `(~'om/get-computed ~actual-props)
            prop-bindings     (when-not (empty? prop-keys)
                                `[{:keys [~@prop-keys]}
                                  ~actual-props])
            computed-bindings (when-not (empty? computed-keys)
                                `[{:keys [~@computed-keys]}
                                  ~actual-computed])]
        (if (or prop-bindings computed-bindings)
          `(~name ~args
            (~'let [~@prop-bindings
                    ~@computed-bindings]
             ~@body))
          `(~name ~args
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
         fns-with-props     (->> (drop-while vector? forms)
                                 (maybe-generate-ident-fn props)
                                 (maybe-generate-key-fn props)
                                 (maybe-generate-query-fn props)
                                 (map resolve-fn-alias)
                                 (map maybe-inject-fn-args)
                                 (map normalize-fn-name)
                                 (map maybe-wrap-render)
                                 (map #(inject-props % props computed)))
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
          (workflo.macros.view/factory ~name ~factory-params))))))

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

     * an Om Next component called User,
     * a component factory called user, with a :keyfn, derived from
       (key ...), and a :validator, derived from (validate ...).

   If the properties spec includes [db [id]], corresponding to
   the Om Next query attribute :db/id, it is assumed that the
   view represents data from DataScript or Datomic. In this case,
   defview will automatically infer (ident ...) and
   (key ...) functions based on the database ID. This behavior
   can be overriden by specifically defining both, ident and key."
  [name & forms]
  (defview* name forms &env))

(ns app-macros.view
  (:require [app-macros.util.string :refer [camel->kebab]]))

(defn parse-props-spec
  "Parse a prop spec such as [user [name email]] into symbols
   that can be used for destructuring properties, e.g.
   [user/name user/email]."
  [spec]
  (letfn [(parse-step [{:keys [props prev] :as ret} prop]
            {:pre [(vector? props)
                   (or (symbol? prop) (vector? prop))
                   (or (nil? prev) (symbol? prev) (vector? prev))]}
            (if (vector? prop)
              (do
                (assert (symbol? prev)
                        (str "The vector " prop " may only appear "
                             "after a symbol, e.g. not after another "
                             "vector or nil: " prev))
                (if (symbol? prev)
                  (let [subprops (map #(symbol (str prev) (str %)) prop)]
                    (-> ret
                        (update :props butlast)
                        (update :props concat subprops)
                        (update :props vec)
                        (assoc :prev prop)))))
              (-> ret
                  (update :props conj prop)
                  (assoc :prev prop))))]
    (:props (reduce parse-step {:props [] :prev nil} spec))))

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
      (let [this-index      (.indexOf args 'this)
            props-index     (.indexOf args 'props)
            actual-props    (if (>= props-index 0)
                              (args props-index)
                              (if (not= scope :static)
                                `(~'om/props ~(args this-index))
                                nil))
            actual-computed `(~'om/get-computed ~actual-props)]
        `(~name ~args
          (~'let [{:keys [~@props]} ~actual-props
                  {:keys [~@computed]} ~actual-computed]
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
         fns-with-args      (map inject-fn-args fns-aliased)
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

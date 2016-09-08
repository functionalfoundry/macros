(ns workflo.macros.view
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [workflo.macros.query.util :refer [capitalized-symbol?]]
            [workflo.macros.query :as q]
            [workflo.macros.query.om-next :as om-query]
            [workflo.macros.specs.query]
            [workflo.macros.specs.view]
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
  [{:keys [form-name]}]
  (let [[scope _ _] (fn-specs form-name)]
    (or scope :instance)))

(defn fn-protocol
  "Return the scope (e.g. (static om.next/IQuery) for a view function."
  [{:keys [form-name]}]
  (let [[_ protocol _ :as fn-spec] (fn-specs form-name)]
    (if fn-spec protocol '(Object))))

(defn fn-args
  "Return the arguments (e.g. [this]) for a view function."
  [{:keys [form-name]}]
  (let [[_ _ args] (fn-specs form-name)]
    (or args '[this])))

(defn fn-alias
  "Return the alias for a view function or the name of the
   function itself if no alias is defined."
  [{:keys [form-name]}]
  (or (fn-aliases form-name) form-name))

(defn resolve-fn-alias
  "Resolve the function alias for a view function."
  [f]
  (let [alias (fn-alias f)]
    (assoc f :form-name alias)))

(defn commands-form?
  "Returns true if the input form is a declaration of view
   commands."
  [{:keys [form-name]}]
  (= 'commands form-name))

(defn generate-command-fn
  "Generate an anonymous wrapper function to call the
   `:run-command` hook with a specific command name."
  [cmd-name]
  `(~'fn
    [~'params & ~'reads]
    (workflo.macros.view/run-command! '~cmd-name ~'this
                                      ~'params ~'reads)))

(defn generate-ident-fn
  "Generate a (ident ...) function from the props spec."
  [props]
  {:form-name 'ident
   :form-body '[[:db/id id]]})

(defn generate-key-fn
  "Generate a (key ...) function from the props spec."
  [props]
  {:form-name 'key
   :form-body '[id]})

(defn maybe-generate-ident-fn
  "If the props spec includes a :db/id property, an (ident ...)
   function is automatically generated based on this ID."
  [props fns]
  (cond-> fns
    (and (some #{'db/id} (map :name props))
         (not (some #{'ident} (map :form-name fns))))
    (conj (generate-ident-fn props))))

(defn maybe-generate-key-fn
  "If the props spec includes a :db/id property, a (key ...)
   function is automatically generated based on this ID."
  [props fns]
  (cond-> fns
    (and (some #{'db/id} (map :name props))
         (not (some #{'keyfn 'key} (map :form-name fns))))
    (conj (generate-key-fn props))))

(defn transform-query-body
  [f]
  (update f :form-body
          (fn [query]
            [(om-query/query (q/parse query))])))

(defn raw-fn?
  "Returns true if f is a raw function, that is if its name
   starts with a ., indicating that its function signature
   should be left alone."
 [{:keys [form-name]}]
 (str/starts-with? (str form-name) "."))

(defn inject-fn-args
  "Inject a function argument binding vector into f if it doesn't
   already have one."
  [f]
  (assoc f :form-args (fn-args f)))

(defn maybe-inject-fn-args
  "Inject a function argument binding vector into f unless it
   is a raw function that is assumed to define its own arguments."
  [f]
  (cond-> f
    (not (raw-fn? f))
    inject-fn-args))

(defn normalize-fn-name
  "Normalize the function name of f. This removes the leading .
   from the names of raw functions."
  [{:keys [form-name] :as f}]
  (assoc f :form-name (symbol (cond-> (str form-name)
                                (raw-fn? f) (subs 1)))))

(defn wrap-render
  "Wraps the body of the function f in a wrapper view according to
   the :wrapper-view option set via workflo.macros.view/configure!."
  [{:keys [form-body] :as f}]
  (assoc f :form-body
         `[((workflo.macros.view/wrapper)
            (~'om.next/props ~'this)
            ~@form-body)]))

(defn maybe-wrap-render
  "If f is the render function, checks whether it has more than one
   child expression. If so, wraps these children in a wrapper view
   according to the :wrapper-view option set via
   workflo.macros.view/configure!."
  [{:keys [form-name form-body] :as f}]
  (cond-> f
    (and (= form-name 'render)
         (> (count form-body) 1))
    wrap-render))

(defn inject-props
  "Wrap the body of a function in a let expression that
   destructures props, computed and command-fns."
  [{:keys [form-body form-args] :as f} props computed command-fns]
  (let [scope (fn-scope f)]
    (if (not= scope :static)
      (let [prop-keys         (q/map-destructuring-keys props)
            computed-keys     (q/map-destructuring-keys computed)
            this-index        (.indexOf form-args 'this)
            props-index       (.indexOf form-args 'props)
            actual-props      (if (>= props-index 0)
                                (form-args props-index)
                                (if (not= scope :static)
                                  `(~'om/props ~(form-args this-index))
                                  nil))
            actual-computed   `(~'om/get-computed ~actual-props)
            prop-bindings     (when-not (empty? prop-keys)
                                `[{:keys [~@prop-keys]}
                                  ~actual-props])
            computed-bindings (when-not (empty? computed-keys)
                                `[{:keys [~@computed-keys]}
                                  ~actual-computed])
            command-bindings  (when (and (>= this-index 0)
                                         (not (empty? command-fns)))
                                (apply concat (into [] command-fns)))]
        (cond-> f
          (or prop-bindings
              computed-bindings
              command-bindings)
          (assoc :form-body
                 `[(~'let [~@prop-bindings
                           ~@computed-bindings
                           ~@command-bindings]
                    ~@form-body)])))
      f)))

(defn anonymous-fn
  "Make a function f anonymous by replacing its name with fn."
  [{:keys [form-args form-body]}]
  `(~'fn ~form-args ~@form-body))

(defn instance-fn
  "Make a function to put inside a record or defui expression."
  [{:keys [form-name form-args form-body]}]
  `(~form-name ~form-args ~@form-body))

(defn instance-fns
  [fns]
  (map instance-fn fns))

(s/fdef defview*
  :args :workflo.macros.specs.view/defview-args
  :ret  any?)

(defn defview*
  ([name forms]
   (defview* name forms nil))
  ([name forms env]
   (let [args-spec      :workflo.macros.specs.view/defview-args
         args           (if (s/valid? args-spec [name forms])
                          (s/conform args-spec [name forms])
                          (throw (Exception.
                                  (s/explain-str args-spec
                                                 [name forms]))))
         forms          (:forms args)
         props          (or (some-> forms :query :form-body q/parse)
                            [])
         computed       (or (some-> forms :computed :form-body q/parse)
                            [])
         commands       (some-> (filter commands-form? (:forms forms))
                                first :form-args)
         command-fns    (zipmap commands
                                (map generate-command-fn
                                     commands))
         fns-with-props (->> (cond-> (vec (:forms forms))
                               (:query forms)
                               (conj (transform-query-body
                                      (:query forms))))
                             (remove commands-form?)
                             (maybe-generate-ident-fn props)
                             (maybe-generate-key-fn props)
                             (map resolve-fn-alias)
                             (map maybe-inject-fn-args)
                             (map normalize-fn-name)
                             (map maybe-wrap-render)
                             (map #(inject-props % props computed
                                                 command-fns)))
         factory-fns    (filter #(= (fn-scope %) :factory)
                                fns-with-props)
         factory-params (zipmap (map (comp keyword :form-name)
                                     factory-fns)
                                (map #(anonymous-fn %)
                                     factory-fns))
         view-fns       (->> fns-with-props
                             (remove #(some #{%} factory-fns))
                             (group-by fn-protocol))
         flat-view-fns  (let [fns (zipmap (keys view-fns)
                                          (map instance-fns
                                               (vals view-fns)))]
                          (apply concat (interleave (keys fns)
                                                    (vals fns))))
         factory-name   (symbol (camel->kebab (str name)))]
     `(do
        (om.next/defui ~name
          ~@flat-view-fns)
        (def ~factory-name
          (workflo.macros.view/factory ~name ~factory-params))
        (register-view! '~name
                        {:view ~name
                         :factory ~factory-name})))))

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

(ns workflo.macros.view
  (:require [clojure.spec :as s]
            [clojure.string :as str]
            [workflo.macros.query.util :refer [capitalized-symbol?]]
            [workflo.macros.query :as q]
            [workflo.macros.query.om-next :as om-query]
            [workflo.macros.specs.query]
            [workflo.macros.specs.view]
            [workflo.macros.util.macro :refer [definition-symbol]]
            [workflo.macros.util.string :refer [camel->kebab]]))

;;;; Om Next query generation

(def fn-specs
  "Function specifications for all functions that are not Object
   instance functions taking [this] as the sole argument."
  '{query-params              [:static (static om.next/IQueryParams) [this]]
    query                     [:static (static om.next/IQuery) [this]]
    ident                     [:instance (static om.next/Ident) [this props]]
    keyfn                     [:factory nil [props]]
    validator                 [:factory nil [props]]
    initLocalState            [:facetory nil [this]]
    componentWillMount        [:instance nil [this]]
    componntDidMount          [:instance nil [this]]
    componentWillUnmount      [:instance nil [this]]
    componentWillReceiveProps [:instance nil [this next-props]]
    componentWillUpdate       [:instance nil [this next-props next-state]]
    componentDidUpdate        [:instance nil [this prev-props prev-state]]
    shouldComponentUpdate     [:instance nil [this next-props next-state]]})

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
  `(~'fn [~'params]
     (workflo.macros.view/run-command! '~cmd-name ~'this ~'params)))

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
    (not (raw-fn? f)) inject-fn-args))

(defn normalize-fn-name
  "Normalize the function name of f. This removes the leading .
   from the names of raw functions."
  [{:keys [form-name] :as f}]
  (assoc f :form-name (symbol (cond-> (str form-name
                                       (raw-fn? f) (subs 1))))))

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
         (> (count form-body) 1)) wrap-render))

(defn wrap-with-query-bindings
  [form-body query query-result]
  `[(~'workflo.macros.bind/with-query-bindings
     ~query ~query-result ~@form-body)])

(defn bind-query-results
  "Wraps the body of a function, binding the values in
   props and computed props to the names used in the
   view query and computed query."
  [{:keys [form-body form-args] :as f} props-query]
  (if (not= :static (fn-scope f))
    (cond-> f
      (not (empty? props-query))
      (update :form-body wrap-with-query-bindings props-query
              (if (some #{'props} form-args)
                `(~'workflo.macros.view/clojurify-props ~'props)
                `(~'workflo.macros.view/clojurify-props
                   ~'(this-as this#
                       (aget this# "props"))))))
    f))

(defn bind-commands
  "Wraps the body of a function in a let that makes the
   view commands available to the body via their names."
  [{:keys [form-body form-args] :as f} commands]
  (if-not (= :static (fn-scope f))
    (let [cmd-bindings (when (some #{'this} form-args)
                         (mapcat (juxt (comp symbol name) generate-command-fn) commands))]
      (cond-> f
        (not (empty? cmd-bindings))
        (assoc :form-body `[(~'let [~@cmd-bindings] ~@form-body)])))
    f))

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

(defn make-anon-fn
  [{:keys [form-args form-body]}]
  `(fn [~@form-args] ~@form-body))

(defn make-react-class
  [view-name args]
  (let [forms       (get args :forms)
        fns         (into {} (comp (map second)
                                   (map (juxt (comp keyword :form-name) identity)))
                             (get forms :forms))
        props-query (some-> (get-in forms [:query :form-body]) (q/parse))
        commands    (get-in fns [:commands :form-args])
        fns         (into {} (remove (comp #{:commands} first)) fns)]
    (->> {:displayName (name view-name)
          :render      (some-> (get fns :render)
                               (maybe-inject-fn-args)
                               (maybe-wrap-render)
                               (bind-query-results props-query)
                               (bind-commands commands)
                               (make-anon-fn))}
         (filter (comp some? second))
         (into {}))))

(defn make-view-definition
  [view-name args]
  (let [def-sym (definition-symbol view-name)
        query   (some-> (get-in args [:forms :query :form-body])
                        (q/parse)
                        (om-query/query))]
    `(def ~def-sym
       {:query ~query
        :class (-> ~(make-react-class view-name args)
                   (~'clj->js)
                   (~'js/React.createClass))})))

(defn make-view-factory
  [view-name args]
  (let [fn-sym  (symbol (camel->kebab (name view-name)))
        def-sym (definition-symbol view-name)]
    `(defn ~fn-sym [~'props & ~'children]
       (js/React.createElement (get ~def-sym :class) ~'props ~'children))))

(defn make-register-call
  [view-name args]
  (let [def-sym (definition-symbol view-name)]
    `(register-view! '~view-name ~def-sym)))

(s/fdef defview*
  :args :workflo.macros.specs.view/defview-args
  :ret  any?)

(defn defview*
  ([name forms]
   (defview* name forms nil))
  ([name forms env]
   (let [args-spec :workflo.macros.specs.view/defview-args
         args      (if (s/valid? args-spec [name forms])
                     (s/conform args-spec [name forms])
                     (throw (Exception. (s/explain-str args-spec [name forms]))))]
     `(do
        ~(make-view-definition name args)
        ~(make-view-factory name args)
        ~(make-register-call name args)))))

(defmacro defview
  "Create a new view with the given name.

   Takes an optional query, an optional computed properties \"query\"
   and an arbitrary number of Om Next component functions (such
   as `ident`, `query`, `query-params`, `initLocalState` or `render`)
   and JavaScript object methods, without requiring their protocols or
   argument bindings (like `[this]` or `[props]`) to be included in
   the definition.

   `defview` will wrap the function bodies of all instance functions
   (`ident`, `render`, lifecycle functions, any object methods) so that
   the values in the query result and in computed properties are bound
   to the names appearing in the queries.

   Usage:

   ```
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
   ```

   The above example would define the following:

     * an Om Next component called `User`,
     * a component factory called `user`, with a `:keyfn`,
       derived from `(key ...)`, and a `:validator`, derived
       from `(validate ...)`.

   If the query includes `[db [id]]`, corresponding to the Om Next
   query attribute `:db/id`, it is assumed that the view represents
   data from DataScript or Datomic. In this case, `defview` will
   automatically infer `(ident ...)` and `(key ...)` functions
   based on the database ID. This behavior can be overriden by
   specifically defining both, `ident` and `key`."
  [name & forms]
  (defview* name forms &env))

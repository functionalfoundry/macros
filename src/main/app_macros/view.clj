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

(defn instance-fn?
  "Return whether or not a given function is an Om Next
   component instance function."
  [f]
  (let [static-fns '(query query-params)
        fname      (first f)]
    (not (some #{fname} static-fns))))

(defn props-fn?
  "Return whether or not a given function is a function
   that expects Om Next properties rather than a component.
   Two examples are :keyfn and :validator passed to om/factory."
  [f]
  (some #{(first f)} ['keyfn 'validator]))

(defn inject-props
  "Wrap the body of a function of the form (name [args] body)
   in a let that destructures props and computed props
   according to the destructuring symbols in props and
   computed.

   If first-arg is :computed, it assumes the first argument
   to the function is a component and uses om/props to
   extract the properties. If the first-arg is :props, it
   assumes the first argument is already a props map."
  [[fname fargs & body :as f] first-arg props computed]
  (let [props-result    (case first-arg
                          :component `(~'om/props ~(first fargs))
                          :props     (first fargs))
        computed-result `(~'om/get-computed ~(first fargs))]
    `(~fname ~fargs
      (~'let [{:keys [~@props]} ~props-result
              {:keys [~@computed]} ~computed-result]
       ~@body))))

(defn get-protocol
  "Get the protocol corresponding to f."
  [f]
  (or (get '{query        (static om.next/IQuery)
             query-params (static om.next/IQueryParams)
             ident        (static om.next/Ident)}
           (first f))
      '(Object)))

(defn add-protocol-fns
  "Add a protocol and its functions to an existing sequence of
   function declarations."
  [decls [protocol fns]]
  (concat decls protocol fns))

(defn component-fn-decls
  "Given a list of component functions, inject destructured
   properties into each instance function and build a list
   of protocols / function declarations for use in defui.

   Example:

       (component-fn-decls
         '((ident [this props] [:user/by-name name])
           (render [this] ...))
         [name email]
         [clicked-fn])

   Result:

      '(static om/Ident
        (ident [this props]
          (let [{:keys [name email]} (om/props this)
                {:keys [clicked-fn]} (om/get-computed this)]
            [:user/by-name name]))
        Object
        (render [this]
          ...))"
  [fns props computed]
  (let [fns-with-props  (map (fn [f]
                               (cond-> f
                                 (instance-fn? f)
                                 (inject-props :component
                                               props computed)))
                             fns)
        fns-by-protocol (group-by get-protocol fns-with-props)
        fn-decls        (reduce add-protocol-fns [] fns-by-protocol)]
    fn-decls))

(defn prefixed-fn-name
  "Generate a function name from fn-sym prefixed with the
   kebab-cased name of a view."
  [fn-sym view-name]
  (symbol (str (camel->kebab (name view-name)) "-" fn-sym)))

(defn prefix-fn-name
  "Prefixes the name of the function fn with the name of a view."
  [fn view-name]
  (list (prefixed-fn-name (first fn) view-name)
        (rest fn)))

(defn add-defn
  "Adds a function definition to a list of function definitions."
  [defns [name args & body]]
  (conj defns `(defn ~name ~args ~@body)))

(defn prop-defns
  "Generates a list of function definitions for functions that
   operate on Om Next properties, such as :keyfn or :validator."
  [view-name fns props computed]
  (->> fns
       (map #(inject-props % :props props computed))
       (map #(prefix-fn-name % view-name))
       (reduce add-defn [])))

(defn lookup-fn
  "Look up a function from a list by its name."
  [fns name]
  (first (filter #(= name (first %)) fns)))

(defn factory-params
  "Build parameters for om/factory based on a view name and a
   list of functions that operate on Om Next properties."
  [view-name prop-fns]
  (let [fns (zipmap [:keyfn :validator]
                    [(lookup-fn prop-fns 'keyfn)
                     (lookup-fn prop-fns 'validator)])]
    (reduce (fn [params [fn-key fn]]
              (cond-> params
                (not (nil? fn))
                (assoc fn-key (prefixed-fn-name (first fn) view-name))))
            {}
            fns)))

(defn defview*
  ([name forms]
   (defview* name forms nil))
  ([name forms env]
   (let [prop-specs         (take-while vector? forms)
         fns                (drop-while vector? forms)
         props              (parse-props-spec (first prop-specs))
         computed           (parse-props-spec (second prop-specs))
         prop-fns           (filter #(props-fn? %) fns)
         component-fns      (remove #(props-fn? %) fns)
         prop-defns      (prop-defns name prop-fns props computed)
         component-fn-decls (component-fn-decls component-fns
                                                props
                                                computed)
         factory-params     (factory-params name prop-fns)]
     `(do
        ~@prop-defns
        (om.next/defui ~name
          ~@component-fn-decls)
        (def ~(symbol (camel->kebab (str name)))
          (om.next/factory ~name ~factory-params))))))

(defmacro defview
  "Create a new view with the given name.

   Takes an optional properties spec, an optional computed properties
   spec and an arbitrary number of Om Next component functions (such
   as ident, query, query-params, initLocalState render) and
   JavaScript object methods, without requiring their protocols to
   be included in the definition.

   Based on the properties and computed properties spec, defview
   will wrap the function bodys of all instance functions (ident,
   render, lifecycle functions) in a destructuring let that makes
   the specified properties available inside the function.

   Usage:

      (defview User
        [name email address [street city zipcode]]
        [clicked-fn]
        (keyfn [props] name)
        (validator [props] ...)
        (ident [this props]
          [:user/by-name name])
        (render [this]
          (html
            [:div.user {:onClick clicked-fn}
             [:h2 name \"(\" email \")\"]
             [:ul.address
              [:li street]
              [:li city]
              [:li zipcode]]])))

   The above example would define the following:

     * An Om Next component called User
     * A component factory called user
     * A keyfn function called user-keyfn that is passed
       to the factory
     * A validator function called user-validator that is
       passed to the factory"
  [name & forms]
  (defview* name forms &env))

(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.specs.command]
            [workflo.macros.query :as q]))

;;;; Configuration

(defonce ^:private +configuration+
  (atom {:query nil
         :process-result nil}))

(defn configure!
  "Configures how commands are created with defcommand
   and how they are executed. Supports the following options:

   :query   - a function that takes a parsed query; this function
              is used to query a cache for data that the command
              being executed needs to run.
   :process-result - a function that is called after a command has been
              executed; it takes the data returned from the command
              implementation and handles it in whatever way is
              desirable."
  [{:keys [query process-result] :as options}]
  (swap! +configuration+ assoc
         :query query
         :process-result process-result))

(defn get-config
  "Returns the configuration for a given configuration key, e.g.
   :query or :process-result."
  [key]
  (@+configuration+ key))

;;;; Command registry

(defonce ^:private +registry+ (atom {}))

(defn register-command!
  [cmd-name env]
  (let [cmd-sym        (util/unqualify cmd-name)
        definition-sym (util/prefix-form-name 'definition cmd-sym)
        definition     (symbol (str (ns-name *ns*))
                               (str definition-sym))]
    (swap! +registry+ assoc cmd-name definition)))

(defn registered-commands
  []
  @+registry+)

(defn resolve-command-sym
  [cmd-name]
  (let [cmd-sym (get @+registry+ cmd-name)]
    (when (nil? cmd-sym)
      (let [err-msg (str "Failed to resolve command '" cmd-name "'")]
        (throw (Exception. err-msg))))
    cmd-sym))

(defn resolve-command
  [cmd-name]
  @(resolve (resolve-command-sym cmd-name)))

(defn run-command
  [cmd-name data]
  (let [definition (resolve-command cmd-name)]
    (when (:data-spec definition)
      (assert (s/valid? (:data-spec definition) data)
              (str "Command data is invalid:"
                   (s/explain-str (:data-spec definition) data))))
    (let [cache-query    (some-> definition :cache-query
                                 (q/bind-query-parameters data))
          query-result   (some-> (get-config :query)
                                 (apply [cache-query]))
          command-result ((:implementation definition)
                          query-result data)]
      (if (get-config :process-result)
        (-> (get-config :process-result)
            (apply [command-result]))
        command-result))))

;;;; The defcommand macro

(s/fdef defcommand*
  :args :workflo.macros.specs.command/defcommand-args
  :ret  ::s/any)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args        (s/conform
                      :workflo.macros.specs.command/defcommand-args
                      [name forms])
         description (:description (:forms args))
         inputs      (:inputs (:forms args))
         forms       (:forms (:forms args))
         impl        (:implementation (:forms args))
         cache-query (when (= 2 (count inputs))
                       (q/parse (second (first inputs))))
         query-keys  (some-> cache-query q/map-destructuring-keys)
         data-spec   (second (last inputs))
         name-sym    (util/unqualify name)
         all-forms   (conj forms {:form-name 'implementation
                                  :form-body (list impl)})
         form-fns    (->> all-forms
                          (map #(update % :form-name
                                        util/prefix-form-name
                                        name-sym))
                          (map #(cond-> %
                                  query-keys (update :form-body
                                                     util/bind-query-keys
                                                     query-keys)))
                          (map util/form->defn))
         all-forms   (cond-> all-forms
                       cache-query (conj {:form-name 'cache-query})
                       data-spec   (conj {:form-name 'data-spec}))
         forms-map   (zipmap (map (comp keyword :form-name) all-forms)
                             (map (fn [form]
                                    (symbol (str (ns-name *ns*))
                                            (str (util/prefix-form-name
                                                  (:form-name form)
                                                  name-sym))))
                                  all-forms))
         query-form  (when cache-query
                       `((~'def ~(util/prefix-form-name 'cache-query
                                                        name-sym)
                          '~cache-query)))
         spec-form   (when data-spec
                       `((~'def ~(util/prefix-form-name 'data-spec
                                                        name-sym)
                          ~data-spec)))
         definition  `(~'def ~(util/prefix-form-name 'definition
                                                     name-sym)
                       ~forms-map)]
     `(do
        ~@form-fns
        ~@query-form
        ~@spec-form
        ~definition))))

(defmacro defcommand
  [name & forms]
  (register-command! name &env)
  (defcommand* name forms &env))

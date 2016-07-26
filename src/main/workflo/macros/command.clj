(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.specs.command]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

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
  [cmd-name cmd-def]
  (swap! +registry+ assoc cmd-name cmd-def))

(defn registered-commands
  []
  @+registry+)

(defn resolve-command
  [cmd-name]
  (let [cmd-def (get @+registry+ cmd-name)]
    (when (nil? cmd-def)
      (let [err-msg (str "Failed to resolve command '" cmd-name "'")]
        (throw (Exception. err-msg))))
    cmd-def))

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
         impl        (:implementation (:forms args))
         cache-query (when (= 2 (count inputs))
                       (q/parse (second (first inputs))))
         query-keys  (some-> cache-query q/map-destructuring-keys)
         data-spec   (second (last inputs))
         name-sym    (unqualify name)
         forms       (cond-> (:forms (:forms args))
                       true        (conj {:form-name 'implementation
                                          :form-body (list impl)})
                       description (conj {:form-name 'description})
                       cache-query (conj {:form-name 'cache-query})
                       data-spec   (conj {:form-name 'data-spec}))
         form-fns    (->> forms
                          (remove (comp nil? :form-body))
                          (map #(update % :form-name
                                        f/prefixed-form-name
                                        name-sym))
                          (map #(assoc % :form-args
                                       '[query-result data]))
                          (map #(cond-> %
                                  query-keys (update :form-body
                                                     util/bind-query-keys
                                                     query-keys)))
                          (map f/form->defn))
         def-sym     (f/qualified-form-name 'definition name-sym)]
     `(do
        ~@form-fns
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when cache-query
            `((~'def ~(f/prefixed-form-name 'cache-query name-sym)
               '~cache-query)))
        ~@(when data-spec
            `(~(f/make-def name-sym 'data-spec data-spec)))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-command! '~name ~def-sym)))))

(defmacro defcommand
  [name & forms]
  (defcommand* name forms &env))

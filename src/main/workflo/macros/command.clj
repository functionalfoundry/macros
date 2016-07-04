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

(defonce ^:private command-registry (atom {}))

(defn register-command!
  [cmd-name]
  (let [cmd-sym        (util/unqualify cmd-name)
        definition-sym (util/prefix-form-name 'definition cmd-sym)
        definition     (resolve definition-sym)]
    (swap! command-registry assoc name definition)))

(defn resolve-command
  [cmd-name]
  (get @command-registry cmd-name))

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
                                  :form-body impl})
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
                       cache-query (conj {:form-name 'cache-query
                                          :form-body '()}))
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
                          ~cache-query)))
         definition  `(~'def ~(util/prefix-form-name 'definition
                                                     name-sym)
                       ~forms-map)]
     `(do
        ~@form-fns
        ~@query-form
        ~definition))))

(defmacro defcommand
  [name & forms]
  (let [cmd-expr (defcommand* name forms &env)]
    (register-command! name)
    cmd-expr))

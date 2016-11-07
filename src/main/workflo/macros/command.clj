(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.command]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

;;;; Configuration options for the defcommand macro

(defconfig command
  ;; Supports the following options:
  ;;
  ;; :query - a function that takes a parsed query and the context
  ;;          that was passed to `run-command!` by the caller;
  ;;          this function is used to query a cache for data that
  ;;          the command being executed needs to run.
  ;;
  ;; :process-emit - a function that is called after a command has
  ;;                 been executed; it takes the data returned from
  ;;                 the command emit function as well as the context
  ;;                 that was passed to `run-command` by the caller
  ;;                 and processes or transforms the command output in
  ;;                 whatever way is desirable.
  {:query nil
   :process-emit nil})

;;;; Command registry

(defregistry command)

;;;; Command execution

(defn run-command!
  ([cmd-name data]
   (run-command! cmd-name data nil))
  ([cmd-name data context]
   (let [definition (resolve-command cmd-name)]
     (when (:spec definition)
       (assert (s/valid? (:spec definition) data)
               (str "Command data is invalid:"
                    (s/explain-str (:spec definition) data))))
     (let [query          (some-> definition :query
                                  (q/bind-query-parameters data))
           query-result   (when query
                            (some-> (get-command-config :query)
                                    (apply [query context])))
           command-result ((:emit definition) query-result data)]
       (if (get-command-config :process-emit)
         (-> (get-command-config :process-emit)
             (apply [command-result context]))
         command-result)))))

;;;; The defcommand macro

(s/fdef defcommand*
  :args :workflo.macros.specs.command/defcommand-args
  :ret  any?)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args-spec   :workflo.macros.specs.command/defcommand-args
         args        (if (s/valid? args-spec [name forms])
                       (s/conform args-spec [name forms])
                       (throw (Exception.
                               (s/explain-str args-spec
                                              [name forms]))))
         description (:description (:forms args))
         query       (some-> args :forms :query :form-body q/parse)
         spec        (some-> args :forms :spec :form-body)
         name-sym    (unqualify name)
         forms       (cond-> (:forms (:forms args))
                       true        (conj (:emit (:forms args)))
                       description (conj {:form-name 'description})
                       query       (conj {:form-name 'query})
                       spec        (conj {:form-name 'spec}))
         form-fns    (->> forms
                          (remove (comp nil? :form-body))
                          (map #(update % :form-name
                                        f/prefixed-form-name
                                        name-sym))
                          (map #(assoc % :form-args
                                       '[query-result data]))
                          (map #(cond-> %
                                  (not (empty? query))
                                  (update :form-body
                                          util/wrap-with-query-bindings
                                          query)))
                          (map f/form->defn))
         def-sym     (f/qualified-form-name 'definition name-sym)]
     `(do
        ~@form-fns
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when query
            `((~'def ~(f/prefixed-form-name 'query name-sym)
               '~query)))
        ~@(when spec
            `(~(f/make-def name-sym 'spec spec)))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-command! '~name ~def-sym)))))

(defmacro defcommand
  [name & forms]
  (defcommand* name forms &env))

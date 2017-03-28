(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.command.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.hooks :refer [defhooks]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.query :as specs.query]
            [workflo.macros.specs.command]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

;;;; Command hooks

;; The following hooks are supported:
;;
;; :query - a function that takes a parsed query and the context
;;          that was passed to `run-command!` by the caller;
;;          this function is used to query a cache for data that
;;          the command being executed needs to run.
;;
;; :auth-query - a function that takes a parsed auth query and the context
;;               that was passed to `run-command!` by the caller;
;;               this function is used to query a cache for data that
;;               is needed to authorize the command execution.
;;
;; :before-emit - a function that is called just before a command emit
;;                implementation is executed; gets passed the command
;;                name, the command query result, the command data and
;;                the command context; the return value of this hook
;;                has no effect
;;
;; :process-emit - a function that is called after a command has
;;                 been executed; it takes the data returned from
;;                 the command emit function as well as the context
;;                 that was passed to `run-command` by the caller
;;                 and processes or transforms the command output in
;;                 whatever way is desirable.

(defhooks command)

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
     (let [bind-data         (if (map? data)
                               (merge context data)
                               (merge context {:data data}))
           query             (some-> (:query definition)
                                     (q/bind-query-parameters bind-data))
           query-result      (when query
                               (-> (process-command-hooks :query {:query query
                                                                  :context context})
                                   (get :query-result)))
           auth-query        (some-> (:auth-query definition)
                                     (q/bind-query-parameters bind-data))
           auth-query-result (when query
                               (-> (process-command-hooks :auth-query {:query query
                                                                       :context context})
                                   (get :query-result)))
           authorized?       (if-let [auth-fn (:auth definition)]
                               (auth-fn query-result auth-query-result)
                               true)]
       (if authorized?
         (do
           (process-command-hooks :before-emit {:command cmd-name
                                                :query-result query-result
                                                :data data
                                                :context context})
           (let [output ((:emit definition) query-result data)]
             (-> (process-command-hooks :process-emit {:command cmd-name
                                                       :output output
                                                       :context context})
                 (get :output))))
         (throw (ex-info (str "Not authorized to run command: " cmd-name)
                         {:command cmd-name
                          :data data
                          :context context})))))))

;;;; The defcommand macro

(s/fdef defcommand*
  :args :workflo.macros.specs.command/defcommand-args
  :ret  any?)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args-spec         :workflo.macros.specs.command/defcommand-args
         args              (if (s/valid? args-spec [name forms])
                             (s/conform args-spec [name forms])
                             (throw (Exception.
                                     (s/explain-str args-spec
                                                    [name forms]))))
         description       (:description (:forms args))
         hints             (:form-body (:hints (:forms args)))
         spec              (some-> args :forms :spec :form-body)
         query             (some-> args :forms :query :form-body q/parse)
         target-cljs?      (boolean (:ns env))
         auth-query        (when-not target-cljs?
                             (some-> args :forms :auth-query :form-body))
         parsed-auth-query (when (and auth-query (not target-cljs?))
                             (q/parse auth-query))
         auth              (when-not target-cljs?
                             (some-> args :forms :auth :form-body))
         name-sym          (unqualify name)
         forms             (cond-> (:forms (:forms args))
                             true        (conj (:emit (:forms args)))
                             description (conj {:form-name 'description})
                             hints       (conj {:form-name 'hints})
                             spec        (conj {:form-name 'spec})
                             query       (conj {:form-name 'query})
                             auth-query  (conj {:form-name 'auth-query})
                             auth        (conj {:form-name 'auth}))
         form-fns          (->> forms
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
         def-sym           (f/qualified-form-name 'definition name-sym)]
     `(do
        ~@form-fns
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when hints
            `(~(f/make-def name-sym 'hints hints)))
        ~@(when query
            `((~'def ~(f/prefixed-form-name 'query name-sym)
               '~query)))
        ~@(when auth-query
            `((~'def ~(f/prefixed-form-name 'auth-query name-sym)
               '~parsed-auth-query)))
        ~@(when auth
            `(~(f/make-defn name-sym 'auth
                 '[query-result auth-query-result]
                 (cond
                   (and query auth-query)
                   `((workflo.macros.bind/with-query-bindings
                       ~query ~'query-result
                       (workflo.macros.bind/with-query-bindings
                         ~parsed-auth-query ~'auth-query-result
                         ~@auth)))

                   auth-query
                   `((workflo.macros.bind/with-query-bindings
                       ~parsed-auth-query ~'auth-query-result
                       ~@auth))

                   query
                   `((workflo.macros.bind/with-query-bindings
                       ~query ~'query-result
                       ~@auth))

                   :else
                   auth))))
        ~@(when spec
            `(~(f/make-def name-sym 'spec spec)))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-command! '~name ~def-sym)))))

(defmacro defcommand
  [name & forms]
  (defcommand* name forms &env))

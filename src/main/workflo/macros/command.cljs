(ns workflo.macros.command
  (:require-macros [workflo.macros.command :refer [defcommand]])
  (:require [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.query :as specs.query]))

;;;; Configuration options for the defcommand macro

(defconfig command
  ;; Supports the following options:
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
  ;; :process-emit - a function that is called after a command has
  ;;                 been executed; it takes the data returned from
  ;;                 the command emit function as well as the context
  ;;                 that was passed to `run-command` by the caller
  ;;                 and processes or transforms the command output in
  ;;                 whatever way is desirable.
  {:query nil
   :auth-query nil
   :process-emit nil})

;;;; Command registry

(defregistry command)

;;;; Query utilities

(defn valid-query?
  [query]
  (s/valid? ::specs.query/query query))

(defn conform-and-parse
  [query]
  (q/conform-and-parse query))

(defn bind-query-parameters
  [query & data-maps]
  (q/bind-query-parameters query (apply merge data-maps)))

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
     (let [bind-data         (merge context data)
           query-result      (when-let [query-hook (get-command-config :query)]
                               (some-> (:query definition)
                                       (q/bind-query-parameters bind-data)
                                       (query-hook context)))
           auth-query-result (when-let [query-hook (get-command-config :auth-query)]
                               (some-> (:auth-query definition)
                                       (q/bind-query-parameters bind-data)
                                       (query-hook context)))
           authorized?       (if-let [auth-fn (:auth definition)]
                               (auth-fn query-result auth-query-result)
                               true)]
       (if authorized?
         (let [emit-output ((:emit definition) query-result data)]
           (if (get-command-config :process-emit)
             (-> (get-command-config :process-emit)
                 (apply [emit-output context]))
             emit-output))
         (throw (ex-info (str "Not authorized to run command: " cmd-name)
                         {:cmd-name cmd-name
                          :data data
                          :context context})))))))

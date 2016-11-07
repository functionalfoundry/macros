(ns workflo.macros.command
  (:require-macros [workflo.macros.command :refer [defcommand]])
  (:require [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]))

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

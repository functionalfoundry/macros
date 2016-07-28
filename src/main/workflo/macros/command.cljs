(ns workflo.macros.command
  (:require-macros [workflo.macros.command])
  (:require [cljs.spec :as s]
            [workflo.macros.config :refer-macros [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer-macros [defregistry]]))

;;;; Configuration options for the defcommand macro

(defconfig command
  ;; Supports the following options:
  ;;
  ;; :query - a function that takes a parsed query; this function
  ;;          is used to query a cache for data that the command
  ;;          being executed needs to run.
  ;;
  ;; :process-emit - a function that is called after a command has
  ;;                 been executed; it takes the data returned from
  ;;                 the command implementation and handles it in
  ;;                 whatever way is desirable.
  {:query nil
   :process-emit nil})

;;;; Command registry

(defregistry command)

;;;; Command execution

(defn run-command!
  [cmd-name data]
  (let [definition (resolve-command cmd-name)]
    (when (:data-spec definition)
      (assert (s/valid? (:data-spec definition) data)
              (str "Command data is invalid:"
                   (s/explain-str (:data-spec definition) data))))
    (let [query          (some-> definition :query
                                 (q/bind-query-parameters data))
          query-result   (when query
                           (some-> (get-command-config :query)
                                   (apply [query])))
          command-result ((:emit definition) query-result data)]
      (if (get-command-config :process-emit)
        (-> (get-command-config :process-emit)
            (apply [command-result]))
        command-result))))

(ns workflo.macros.command
  (:require-macros [workflo.macros.command])
  (:require [cljs.spec :as s]
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
  [cmd-name cmd-def]
  (swap! +registry+ assoc cmd-name cmd-def))

(defn registered-commands
  []
  @+registry+)

(defn resolve-command
  [cmd-name]
  (let [cmd-sym (get @+registry+ cmd-name)]
    (when (nil? cmd-sym)
      (let [err-msg (str "Failed to resolve command '" cmd-name "'")]
        (throw (js/Error. err-msg))))
    cmd-sym))

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

(ns workflo.macros.view
  (:require [om.next :as om]
            [om.dom]
            [workflo.macros.bind]
            [workflo.macros.config :refer-macros [defconfig]]
            [workflo.macros.registry :refer-macros [defregistry]]))

;;;; Configuration options for the defview macro

(defn default-run-command!
  "Default command handler, generating an Om Next transaction
   with a mutation and queries that correspond 1:1 to the
   command, its parameters and the optional reads."
  [cmd-name view params reads]
  (om/transact! view `[(~cmd-name ~{:cmd-params params})
                       ~@(or reads [])]))

(defconfig view
  ;; Supports the following options:
  ;;
  ;; :wrapper-view - a React element factory to use for wrapping
  ;;                 the body of render functions if render has
  ;;                 more than a single child expression.
  ;;
  ;; :run-command - a function that takes a view, a command name
  ;;                and a parameter map and an optional vector
  ;;                of things to re-query after running the
  ;;                command.
  {:wrapper-view nil
   :run-command default-run-command!})

(defn wrapper
  "Returns a wrapper factory for use in render functions. If no
   wrapper function is defined, issues a warning and returns
   om.dom/div to avoid breaking apps entirely."
  []
  (if-not (get-view-config :wrapper-view)
    (do (js/console.warn "No wrapper view defined for defview.")
        om.dom/div)
    (get-view-config :wrapper-view)))

(defn run-command!
  "Runs a given command using the configured :run-command
   handler, if defined."
  [cmd-name view params reads]
  (if-not (get-view-config :run-command)
    (js/console.warn "No :run-command handler defined for defview.")
    (some-> (get-view-config :run-command)
            (apply [cmd-name view params reads]))))

(defn factory
  "A wrapper factory around om.next/factory that makes the nil
   argument for properties optional."
  [& args]
  (let [om-factory (apply om.next/factory args)]
    (fn [& children]
      (if (or (map? (first children))
              (object? (first children))
              (nil? (first children)))
        (apply (partial om-factory (first children))
               (rest children))
        (apply (partial om-factory {})
               children)))))

;;;; View registry

(defregistry view)

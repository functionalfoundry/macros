(ns workflo.macros.view
  (:require [om.next]
            [om.dom]))

;;;; Configuration

(def +configuration+
  "Configuration options for the defview macro."
  (atom {:wrapper-view nil}))

(defn configure!
  "Configures the defview macro, usually before it is being used.
   Supports the following options:

   :wrapper-view - a React element factory to use for wrapping
                   the body of render functions if render has
                   more than a single child expression."
  [{:keys [wrapper-view] :as options}]
  (swap! +configuration+ assoc
         :wrapper-view wrapper-view))

(defn get-config
  "Returns the configuration for a given configuration key, e.g.
   :wrapper-view."
  [key]
  (@+configuration+ key))

(defn wrapper
  "Returns a wrapper factory for use in render functions. If no
   wrapper function is defined, issues a warning and returns
   om.dom/div to avoid breaking apps entirely."
  []
  (if-not (get-config :wrapper-view)
    (do (js/console.warning "No wrapper view defined for defview.")
        om.dom/div)
    (get-config :wrapper-view)))

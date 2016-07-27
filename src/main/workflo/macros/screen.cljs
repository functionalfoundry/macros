(ns workflo.macros.screen
  (:require [workflo.macros.registry :refer-macros [defregistry]]
            [workflo.macros.util.js :refer [resolve]]))

;;;; Configuration options for the defscreen macro

(defonce +configuration+
  (atom {}))

(defn configure!
  "Configures the defscreen macro, usually before it is being used.
   Supports the following options:

   tbd."
  [{:keys [] :as options}]
  (swap! +configuration+ assoc))

(defn get-config
  "Returns the configuration for a given configuration key."
  [key]
  (@+configuration+ key))

;;;; Screen registry

(defregistry screen)

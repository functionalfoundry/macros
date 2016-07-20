(ns workflo.macros.screen
  (:require [workflo.macros.util.js :refer [resolve]]))

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

(defonce ^:private +registry+ (atom (sorted-map)))

(defn register-screen!
  [screen-name def-sym]
  (swap! +registry+ assoc screen-name def-sym))

(defn reset-registry!
  []
  (reset! +registry+ (sorted-map)))

(defn registered-screens
  []
  @+registry+)

(defn resolve-screen-sym
  [screen-name]
  (let [screen-sym (get @+registry+ screen-name)]
    (when (nil? screen-sym)
      (let [err-msg (str "Failed to resolve screen '" screen-name "'")]
        (throw (js/Exception. err-msg))))
    screen-sym))

(defn sym->js
  "Converts a ClojureScript symbol to a JS variable string."
  [sym]
  (-> sym
      (string/replace #"/" ".")
      (string/replace #"-" "_")))

(defn resolve
  "Resolves a ClojureScript symbol into a JS variable."
  [sym]
  (js/eval (sym->js sym)))

(defn resolve-screen
  [screen-name]
  (resolve-screen-sym screen-name))

(ns app-macros.docs
  (:require [devcards.core :refer-macros [defcard start-devcard-ui!]]
            [app-macros.docs.util.string]))

(enable-console-print!)
(start-devcard-ui!)

(defcard
  "# App Macros

   `app-macros` is a collection of Clojure and ClojureScript macros
   for web and mobile development.")

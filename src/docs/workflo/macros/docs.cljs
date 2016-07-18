(ns workflo.macros.docs
  (:require [devcards.core :refer-macros [defcard start-devcard-ui!]]
            [workflo.macros.docs.util.string]
            [workflo.macros.docs.screen]
            [workflo.macros.docs.view]))

(enable-console-print!)
(start-devcard-ui!)

(defcard
  "# Workflo Macros

   `workflo.macros` is a collection of Clojure and ClojureScript macros
   for web and mobile development.")

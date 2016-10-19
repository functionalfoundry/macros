(ns workflo.macros.screen
  (:require-macros [workflo.macros.screen :refer [defscreen]])
  (:require [workflo.macros.config :refer-macros [defconfig]]
            [workflo.macros.registry :refer-macros [defregistry]]
            [workflo.macros.util.js :refer [resolve]]))

;;;; Configuration options for the defscreen macro

(defconfig screen {})

;;;; Screen registry

(defregistry screen)

(ns workflo.macros.entity
  (:require-macros [workflo.macros.entity :refer [defentity]])
  (:require [workflo.macros.registry :refer [defregistry]]))

;;;; Entity registry

(defregistry entity)

(ns workflo.macros.permission
  (:require-macros [workflo.macros.permission :refer [defpermission]])
  (:require [workflo.macros.registry :refer [defregistry]]))

;;;; Permission registry

(defregistry permission)

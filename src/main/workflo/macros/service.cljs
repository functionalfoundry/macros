(ns workflo.macros.service
  (:require-macros [workflo.macros.service])
  (:require [cljs.spec :as s]
            [workflo.macros.config :refer-macros [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer-macros [defregistry]]))

;;;; Configuration options for the defservice macro

(defconfig service
  ;; Supports the following options:
  ;;
  ;; :query - a function that takes a parsed query; this function
  ;;          is used to query a cache for data that the service
  ;;          being executed needs to run.
  {:query nil})

;;;; Service registry

(defregistry service)

;;;; Delivery to services

;; TODO
(defn deliver!
  [service-name data])

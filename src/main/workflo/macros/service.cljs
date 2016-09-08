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
  ;;          is used to query a data store for data that the service
  ;;          needs to process its input.
  {:query nil})

;;;; Service registry

(defregistry service)

;;;; Service components

(defregistry service-component)

(defn new-service-component
  ([name]
   (new-service-component name {}))
  ([name config]
   (let [service (resolve-service name)]
     ((:component-ctor service) {:service service
                                 :config config}))))

;;;; Service interface

(defprotocol IService
  (process [this query-result data]))

;;;; Delivery to services

(defn deliver-to-service-component!
  [component data]
  (let [query  (some-> component :service :query
                       (q/bind-query-parameters data))
        result (when query
                 (some-> (get-service-config :query)
                         (apply [query])))]
    (process component result data)))

(defn deliver-to-services!
  [data]
  {:pre [(s/valid? (s/map-of keyword? any?) data)]}
  (doseq [[service-kw service-data] data]
    (let [service-name (symbol (name service-kw))
          component    (try
                         (resolve-service-component service-name)
                         (catch js/Error e
                           (println "WARN:" (.-message e))))]
      (some-> component
        (deliver-to-service-component! service-data)))))

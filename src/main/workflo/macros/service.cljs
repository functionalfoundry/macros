(ns workflo.macros.service
  (:require-macros [workflo.macros.service :refer [defservice]])
  (:require [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]))

;;;; Configuration options for the defservice macro

(defconfig service
  ;; Supports the following options:
  ;;
  ;; :query - a function that takes a parsed query and the context
  ;;          that was passed to `deliver-to-service-component!` or
  ;;          `deliver-to-services!` by the caller;
  ;;          this function is used to query a data store for data
  ;;          that the service needs to process its input.
  ;;
  ;; :process-output - a function that takes a service, the context
  ;;                   that was passed to `deliver-to-service-component!`
  ;;                   or `deliver-to-services!` by the caller, and
  ;;                   the output of a call to the service's `process`
  ;;                   implementation;
  ;;                   this function can be used to further process
  ;;                   the output of the service.
  {:query nil
   :process-output nil})

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
  (process [this query-result data context]))

;;;; Delivery to services

(defn deliver-to-service-component!
  ([component data]
   (deliver-to-service-component! component data nil))
  ([component data context]
   (let [query   (some-> component :service :query
                         (q/bind-query-parameters data))
         qresult (when query
                   (some-> (get-service-config :query)
                           (apply [query context])))
         output  (process component qresult data context)]
     (cond->> output
       (fn? (get-service-config :process-output))
       ((get-service-config :process-output)
        (:service component) context)))))

(defn deliver-to-services!
  ([data]
   (deliver-to-services! data nil))
  ([data context]
   (doseq [[service-kw service-data] data]
     (let [service-name (symbol (name service-kw))
           component    (try
                          (resolve-service-component service-name)
                          (catch js/Error error
                            (js/console.warn "Failed to resolve service component:"
                                             error)))]
       (some-> component
               (deliver-to-service-component! service-data context))))))

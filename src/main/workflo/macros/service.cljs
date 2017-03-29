(ns workflo.macros.service
  (:require-macros [workflo.macros.service :refer [defservice]])
  (:require [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.hooks :refer [defhooks]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]))

;;;; Service hooks

;; The following hooks are supported:
;;
;; :query - a function that takes a parsed query and the context
;;          that was passed to `deliver-to-service-component!` or
;;          `deliver-to-services!` by the caller;
;;          this function is used to query a data store for data
;;          that the service needs to process its input.
;;
;; :deliver - a function that takes a service component, a query result,
;;            the data that will be delivered to the service as well as
;;            the context that that was passed to
;;            `deliver-to-service-component!` or `deliver-to-services!`.
;;
;; :process-output - a function that takes a service, the context
;;                   that was passed to `deliver-to-service-component!`
;;                   or `deliver-to-services!` by the caller, and
;;                   the output of a call to the service's `process`
;;                   implementation;
;;                   this function can be used to further process
;;                   the output of the service.
(defhooks service)

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
   (let [query   (some-> component :service :query (q/bind-query-parameters data))
         qresult (when query
                   (-> (process-service-hooks :query {:query query
                                                      :context context})
                       (get :query-result)))
         _       (process-service-hooks :deliver {:component component
                                                  :query-result qresult
                                                  :data data
                                                  :context context})
         output  (process component qresult data context)]
     (-> (process-service-hooks :process-output {:service (:service component)
                                                 :output output
                                                 :context context})
         (get :output)))))

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
       (some-> component (deliver-to-service-component! service-data context))))))

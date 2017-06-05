(ns workflo.macros.service
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [workflo.macros.service :refer [defservice]])
  (:require [clojure.core.async :refer [<! chan put! timeout]]
            [clojure.spec :as s]
            [workflo.macros.bind]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.hooks :refer [defhooks]]
            [workflo.macros.query :as q]
            [workflo.macros.registry :refer [defregistry]]))

;;;; Service configuration

(def ^:private +default-async-delay+ 500)

(defconfig service
  {:async-delay +default-async-delay+})

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
         data    (-> (process-service-hooks :deliver {:component component
                                                      :query-result qresult
                                                      :data data
                                                      :context context})
                     (get :data))
         output  (process component qresult data context)]
     (-> (process-service-hooks :process-output {:service (:service component)
                                                 :output output
                                                 :context context})
         (get :output)))))

(defn deliver-now! [service data]
  (when-let [component (try
                         (resolve-service-component (:name service))
                         (catch js/Error error
                           (js/console.warn (str "Failed to resolve service "
                                                 "component '" (:name service) "'"))))]
    (try
      (deliver-to-service-component! component data)
      (catch js/Error error
        (js/console.warn (str "Failed to deliver to service '" (:name service) "'" error))))))

(defn deliver-later! [service data]
  (go
    (<! (timeout (let [delay (get-service-config :async-delay)]
                   (if (pos? delay) delay +default-async-delay+))))
    (deliver-now! service data)))

;;;; Debounced deliveries

(defn debounced-chan
  [in ms]
  (let [out (chan)]
    (go
      (loop [last-val nil]
        (let [val          (if (nil? last-val) (<! in) last-val)
              timer        (timeout ms)
              [new-val ch] (alts! [in timer])]
          (condp = ch
            timer (do (>! out val)
                      (recur nil))
            in    (if new-val
                    (recur new-val))))))
    out))

(def +service-data-chans+ (atom {}))
(def +debounced-service-data-chans+ (atom {}))

(defn setup-debounce-loop [ch service data]
  (go-loop []
    (let [_ (<! ch)]
      (deliver-now! service data)
      (recur))))

(defn deliver-debounced-by-data! [service data]
  (let [key [(:name service) data]]
    ;; Ensure the service/data channels for this service/data combination exists
    (when-not (get (deref +service-data-chans+) key)
      (swap! +service-data-chans+ assoc key (chan)))
    (when-not (get (deref +debounced-service-data-chans+) key)
      (swap! +debounced-service-data-chans+ assoc key
             (let [input-ch (get (deref +service-data-chans+) key)]
               (doto (debounced-chan input-ch 1000)
                 (setup-debounce-loop service data)))))
    ;; Queue a trigger event for this service/data combination
    (put! (get (deref +service-data-chans+) key) :trigger)))

;;;; Delivery entry points

(defn deliver-to-services!
  ([data]
   (deliver-to-services! data nil))
  ([data context]
   (doseq [[service-kw service-data] data]
     (let [service-name (symbol (name service-kw))]
       (try
         (let [service (resolve-service service-name)
               hints   (:hints service)]
           (cond
             (some #{:async} hints)
             (deliver-later! service service-data)

             (some #{:debounced-by-data} hints)
             (deliver-debounced-by-data! service service-data)

             :else (deliver-now! service service-data)))
         (catch js/Error error
           (js/console.warn (str "Failed to resolve service '" service-name "'") error)))))))

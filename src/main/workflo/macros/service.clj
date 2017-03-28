(ns workflo.macros.service
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.bind :refer [with-query-bindings]]
            [workflo.macros.command.util :as cutil]
            [workflo.macros.service.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.service]
            [workflo.macros.util.macro :refer [component-record-symbol
                                               definition-symbol
                                               record-symbol]]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.string :refer [kebab->camel]]
            [workflo.macros.util.symbol :refer [unqualify]]))

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
  {:query nil
   :deliver nil
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
         _       (some-> (get-service-config :deliver)
                         (apply [component qresult data context]))
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
                          (catch Exception e
                            (println "WARN:" (.getMessage e))))]
       (some-> component
               (deliver-to-service-component! service-data
                                              context))))))

;;;; The defservice macro

(s/fdef defservice*
  :args :workflo.macros.specs.service/defservice-args
  :ret  any?)

(defn make-service-component
  [name args]
  `(defrecord ~(component-record-symbol name)
       ~'[service config]
     com.stuartsierra.component/Lifecycle
     (~'start [~'this]
      (let [~'this' ((or (:start ~'service) identity) ~'this)]
        (register-service-component! (:name ~'service) ~'this')
        ~'this'))
     (~'stop [~'this]
      (let [~'this' ((or (:stop ~'service) identity) ~'this)]
        (unregister-service-component! (:name ~'service))
        ~'this'))
     workflo.macros.service/IService
     (~'process ~'[this query-result data context]
      (some-> (:process ~'service)
              (apply [~'this ~'query-result ~'data ~'context])))))

(defn make-service-record
  [name args]
  `(defrecord ~(record-symbol name)
       ~'[name description dependencies start stop
          query spec process]))

(defn make-service-definition
  [service-name args]
  (let [forms              (:forms args)
        query              (some-> forms :query :form-body q/parse)
        start              (some-> forms :start :form-body)
        stop               (some-> forms :stop :form-body)
        process            (some-> forms :process :form-body)
        replay?            (some-> forms :replay? :form-body)
        ctor-sym           (symbol (str "map->" (record-symbol
                                                 service-name)))
        component-ctor-sym (symbol (str "map->" (component-record-symbol
                                                 service-name)))]
    `(def ~(symbol (name (definition-symbol service-name)))
       (~ctor-sym
        {:name '~service-name
         :description ~(-> forms :description)
         :dependencies '~(-> forms :dependencies :form-body)
         :replay? ~replay?
         :query '~query
         :spec ~(some-> forms :spec :form-body)
         :start ~(when start
                   `(fn [~'this]
                      ~@start))
         :stop ~(when stop
                  `(fn [~'this]
                     ~@stop))
         :process ~(when process
                     (if query
                       `(fn ~'[this query-result data context]
                          (with-query-bindings ~query ~'query-result
                            ~@process))
                       `(fn ~'[this query-result data context]
                          ~@process)))
         :component-ctor ~component-ctor-sym}))))

(defn make-register-call
  [name args]
  `(register-service! '~name ~(definition-symbol name)))

(defn defservice*
  ([name forms]
   (defservice* name forms nil))
  ([name forms env]
   (let [args-spec :workflo.macros.specs.service/defservice-args
         args      (if (s/valid? args-spec [name forms])
                     (s/conform args-spec [name forms])
                     (throw (Exception.
                             (s/explain-str args-spec
                                            [name forms]))))]
     `(do
        ~(make-service-component name args)
        ~(make-service-record name args)
        ~(make-service-definition name args)
        ~(make-register-call name args)))))

(defmacro defservice
  [name & forms]
  (defservice* name forms &env))

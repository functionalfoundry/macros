(ns workflo.macros.service
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.command.util :as cutil]
            [workflo.macros.service.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.service]
            [workflo.macros.util.macro :refer [component-record-symbol
                                               definition-symbol
                                               record-symbol
                                               with-destructured-query]]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.string :refer [kebab->camel]]
            [workflo.macros.util.symbol :refer [unqualify]]))

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
  {:pre [(s/valid? (s/map-of keyword? ::s/any) data)]}
  (doseq [[service-kw service-data] data]
    (let [service-name (symbol (name service-kw))
          component    (try
                         (resolve-service-component service-name)
                         (catch Exception e
                           (println "WARN:" (.getMessage e))))]
      (some-> component
        (deliver-to-service-component! service-data)))))

;;;; The defservice macro

(s/fdef defservice*
  :args :workflo.macros.specs.service/defservice-args
  :ret  ::s/any)

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
     (~'process ~'[this query-result data]
      (some-> (:process ~'service)
              (apply [~'this ~'query-result ~'data])))))

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
        ctor-sym           (symbol (str "map->" (record-symbol
                                                 service-name)))
        component-ctor-sym (symbol (str "map->" (component-record-symbol
                                                 service-name)))]
    `(def ~(symbol (name (definition-symbol service-name)))
       (~ctor-sym
        {:name '~service-name
         :description ~(-> forms :description)
         :dependencies '~(-> forms :dependencies :form-body)
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
                       `(fn ~'[this query-result data]
                          (with-destructured-query ~query ~'query-result
                            ~@process))
                       `(fn ~'[this query-result data]
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

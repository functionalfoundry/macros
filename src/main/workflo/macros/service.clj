(ns workflo.macros.service
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.service]
            [workflo.macros.util.form :as f]
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

;;;; Delivery to services

;; TODO
(defn deliver!
  [service-name data])

;;;; The defservice macro

(s/fdef defservice*
  :args :workflo.macros.specs.service/defservice-args
  :ret  ::s/any)

(defn defservice*
  ([name forms]
   (defservice* name forms nil))
  ([name forms env]
   (let [args-spec   :workflo.macros.specs.service/defservice-args
         args        (if (s/valid? args-spec [name forms])
                       (s/conform args-spec [name forms])
                       (throw (Exception.
                               (s/explain-str args-spec
                                              [name forms]))))
         description (:description (:forms args))
         query       (some-> args :forms :query :form-body q/parse)
         query-keys  (some-> query q/map-destructuring-keys)
         data-spec   (some-> args :forms :data-spec :form-body)
         name-sym    (unqualify name)
         forms       (cond-> (:forms (:forms args))
                       true        (conj (:process (:forms args)))
                       description (conj {:form-name 'description})
                       query       (conj {:form-name 'query})
                       data-spec   (conj {:form-name 'data-spec}))
         form-fns    (->> forms
                          (remove (comp nil? :form-body))
                          (map #(update % :form-name
                                        f/prefixed-form-name
                                        name-sym))
                          (map #(assoc % :form-args
                                       '[query-result data]))
                          (map #(cond-> %
                                  query-keys (update :form-body
                                                     util/bind-query-keys
                                                     query-keys)))
                          (map f/form->defn))
         def-sym     (f/qualified-form-name 'definition name-sym)]
     `(do
        ~@form-fns
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when query
            `((~'def ~(f/prefixed-form-name 'query name-sym)
               '~query)))
        ~@(when data-spec
            `(~(f/make-def name-sym 'data-spec data-spec)))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-service! '~name ~def-sym)))))

(defmacro defservice
  [name & forms]
  (defservice* name forms &env))

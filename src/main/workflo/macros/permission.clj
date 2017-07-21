(ns workflo.macros.permission
  (:require [clojure.spec.alpha :as s]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.permission]
            [workflo.macros.util.macro :refer [definition-symbol]]))

;;;; Permission registry

(defregistry permission)

;;;; The defpermission macro

(defn make-permission-definition
  [permission-name {:keys [forms]}]
  (let [title       (:form-body (:title forms))
        description (:form-body (:description forms))]
    `(def ~(symbol (name (definition-symbol permission-name)))
       {:name ~(keyword permission-name)
        :title ~title
        :description ~description})))

(defn make-register-call
  [name args]
  `(register-permission! '~name ~(definition-symbol name)))

(s/fdef defpermission*
  :args :workflo.macros.specs.permission/defpermission-args
  :ret  any?)

(defn defpermission*
  ([name forms]
   (defpermission* name forms nil))
  ([name forms env]
   (let [args-spec :workflo.macros.specs.permission/defpermission-args
         args      (if (s/valid? args-spec [name forms])
                     (s/conform args-spec [name forms])
                     (throw (Exception. (s/explain-str args-spec [name forms]))))]
     `(do
        ~(make-permission-definition name args)
        ~(make-register-call name args)))))

(defmacro defpermission
  [name & args]
  (defpermission* name args &env))

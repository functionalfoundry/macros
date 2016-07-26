(ns workflo.macros.screen
  (:require [clojure.spec :as s]
            [clojure.string :as string]
            [workflo.macros.screen.util :as util]
            [workflo.macros.specs.screen]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.string :refer [camel->kebab]]
            [workflo.macros.util.symbol :refer [unqualify]]))

(s/fdef defscreen*
  :args :workflo.macros.specs.screen/defscreen-args
  :ret  ::s/any)

(defn defscreen*
  ([name forms]
   (defscreen* name forms nil))
  ([name forms env]
   (let [args-spec   :workflo.macros.specs.screen/defscreen-args
         args        (if (s/valid? args-spec [name forms])
                       (s/conform args-spec [name forms])
                       (throw (Exception.
                               (s/explain-str args-spec
                                              [name forms]))))
         description (:description (:forms args))
         name-sym    (unqualify name)
         forms       (-> (:forms args)
                         (select-keys [:url :layout])
                         (vals)
                         (cond->
                           true        (conj {:form-name 'name})
                           true        (conj {:form-name 'forms})
                           description (conj {:form-name 'description})))
         field-forms (:forms (:forms args))]
     `(do
        ~(f/make-def-quoted name-sym 'name name)
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~(f/make-def name-sym 'url
          (let [url-str (:form-body (:url (:forms args)))]
            {:string url-str
             :segments (util/url-segments url-str)}))
        ~(f/make-def name-sym 'forms
          (zipmap (map (comp keyword :form-name) field-forms)
                  (map :form-body field-forms)))
        ~(f/make-def name-sym 'layout
          (:form-body (:layout (:forms args))))
        ~(f/make-def name-sym 'definition
          (f/forms-map forms name-sym))
        (register-screen! '~name-sym
                          ~(f/qualified-form-name
                            'definition name-sym))))))


(defmacro defscreen
  "Create a new screen with the given name."
  [name & forms]
  (defscreen* name forms &env))

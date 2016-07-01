(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.specs.command]
            [workflo.macros.query :as q]))

;;;; Command registry

(defonce ^:private command-registry (atom {}))

(defn register-command!
  [cmd-name]
  (let [cmd-sym        (util/unqualify cmd-name)
        definition-sym (util/prefix-form-name 'definition cmd-sym)
        definition     (resolve definition-sym)]
    (swap! command-registry assoc name definition)))

(defn resolve-command
  [cmd-name]
  (get @command-registry cmd-name))

;;;; The defcommand macro

(s/fdef defcommand*
  :args :workflo.macros.specs.command/defcommand-args
  :ret  ::s/any)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args        (s/conform
                      :workflo.macros.specs.command/defcommand-args
                      [name forms])
         description (:description (:forms args))
         inputs      (:inputs (:forms args))
         forms       (:forms (:forms args))
         impl        (:implementation (:forms args))
         cache-query (when (= 2 (count inputs))
                       (q/parse (second (first inputs))))
         query-keys  (some-> cache-query q/map-destructuring-keys)
         data-spec   (second (last inputs))
         name-sym    (util/unqualify name)
         all-forms   (conj forms {:form-name 'implementation
                                  :form-body impl})
         form-fns    (->> all-forms
                          (map #(update % :form-name
                                        util/prefix-form-name
                                        name-sym))
                          (map #(cond-> %
                                  query-keys (update :form-body
                                                     util/bind-query-keys
                                                     query-keys)))
                          (map util/form->defn))
         fns-map     (zipmap (map (comp keyword :form-name) all-forms)
                             (map (fn [form]
                                    (symbol (str (ns-name *ns*))
                                            (str (util/prefix-form-name
                                                  (:form-name form)
                                                  name-sym))))
                                  all-forms))
         definition  `(~'def ~(util/prefix-form-name 'definition
                                                     name-sym)
                       ~fns-map)]
     `(do
        ~@form-fns
        ~definition))))

(defmacro defcommand
  [name & forms]
  (let [cmd-expr (defcommand* name forms &env)]
    (register-command! name)
    cmd-expr))

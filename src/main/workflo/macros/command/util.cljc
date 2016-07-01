(ns workflo.macros.command.util
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [clojure.string :as string]
            [workflo.macros.query]
            [workflo.macros.specs.command]))

(s/def ::unqualified-symbol
  (s/with-gen
    (s/and symbol? #(not (some #{\/} (str %))))
    #(gen/fmap (comp symbol name) (s/gen symbol?))))

(s/fdef unqualify
  :args (s/cat :x symbol?)
  :ret  ::unqualified-symbol)

(defn unqualify
  "Take a symbol and generate a non-namespaced version of
   it by replacing slashes (/) with dashes (-)."
  [x]
  (let [x-ns   (namespace x)
        x-name (string/replace (name x) "/" "")]
    (if x-ns
      (symbol (str x-ns "-" x-name))
      (symbol x-name))))


(s/fdef prefix-form-name
  :args (s/cat :form-name ::unqualified-symbol
               :prefix ::unqualified-symbol)
  :ret  symbol?
  :fn   #(= (-> % :ret)
            (symbol (str (-> % :args :prefix) "-"
                         (-> % :args :form-name)))))

(defn prefix-form-name
  [form-name prefix]
  (symbol (str prefix "-" form-name)))

;;;; Utilities

(s/fdef bind-query-keys
  :args (s/cat :form-body
               (s/and seq?
                      :workflo.macros.specs.command/command-form-body)
               :query-keys
               :workflo.macros.query/map-destructuring-keys)
  :ret  :workflo.macros.specs.command/command-form-body)

(defn bind-query-keys
  [form-body query-keys]
  `((~'let [{:keys ~query-keys} ~'query-result]
     ~@form-body)))

(s/fdef form->defn
  :args (s/cat :form
               :workflo.macros.specs.command/conforming-command-form)
  :ret  (s/cat :defn #{'defn}
               :name ::unqualified-symbol
               :body (s/* ::s/any)))

(defn form->defn
  [form]
  `(~'defn ~(:form-name form)
    [~'query-result ~'data]
    ~@(:form-body form)))

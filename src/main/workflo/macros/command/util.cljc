(ns workflo.macros.command.util
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [clojure.string :as string]))

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
        x-name (name x)]
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

(ns workflo.macros.util.symbol
  (:require [clojure.spec :as s]
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
        x-name (string/replace (name x) "/" "")]
    (if x-ns
      (symbol (str x-ns "-" x-name))
      (symbol x-name))))

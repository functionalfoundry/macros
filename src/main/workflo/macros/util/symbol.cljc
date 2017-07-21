(ns workflo.macros.util.symbol
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
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
   it by replacing slashes (/) and dots (.) with dashes (-)."
  [x]
  (symbol (string/replace (str x) #"[\./]" "-")))

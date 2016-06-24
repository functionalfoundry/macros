(ns workflo.macros.command.util
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [clojure.string :as string]))

(s/fdef unqualify
  :args (s/cat :x symbol?)
  :ret  (s/and symbol? #(not (some #{\/} (str %)))))

(defn unqualify
  "Take a symbol and generate a non-namespaced version of
   it by replacing slashes (/) with dashes (-)."
  [x]
  (let [x-ns   (namespace x)
        x-name (name x)]
    (if x-ns
      (symbol (str x-ns "-" x-name))
      (symbol x-name))))

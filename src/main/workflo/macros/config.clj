(ns workflo.macros.config
  (:require [clojure.spec :as s]
            [inflections.core :refer [plural]]))

(s/def ::defconfig-args
  (s/cat :name symbol?
         :options (s/map-of keyword? ::s/any)))

(s/fdef defconfig*
  :args ::defconfig-args
  :ret  ::s/any)

(defn defconfig*
  ([name options]
   (defconfig* name options nil))
  ([name options env]
   (let [config-sym     (symbol (str "+" name "-config+"))
         configure-sym  (symbol (str "configure-" (plural name) "!"))
         get-config-sym (symbol (str "get-" name "-config"))]
     `(do
        (defonce ^:private ~config-sym (atom ~options))
        (defn ~configure-sym
          [~'options]
          (swap! ~config-sym merge ~'options))
        (defn ~get-config-sym
          ([]
           (deref ~config-sym))
          ([~'option]
           (get (~get-config-sym) ~'option)))))))

(defmacro defconfig
  [name options]
  (defconfig* name options &env))

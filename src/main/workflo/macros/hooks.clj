(ns workflo.macros.hooks
  (:require [clojure.spec :as s]))

(s/fdef defhooks*
  :args (s/cat :name symbol?)
  :ret  any?)

(defn defhooks*
  ([name]
   (defhooks* name nil nil))
  ([name env]
   (defhooks* name nil env))
  ([name callback env]
   (let [hooks-sym      (symbol (str "+" name "-hooks+"))
         register-sym   (symbol (str "register-" name "-hook!"))
         unregister-sym (symbol (str "unregister-" name "-hook!"))
         registered-sym (symbol (str "registered-" name "-hooks"))
         reset-sym      (symbol (str "reset-registered-" name "-hooks!"))
         process-sym    (symbol (str "process-" name "-hooks"))]
     `(do
        (defonce ^:private ~hooks-sym (atom (sorted-map)))
        (defn ~register-sym [~'name ~'hook]
          (swap! ~hooks-sym update ~'name (comp set conj) ~'hook)
          ~@(when callback
              `((~callback :register ~'name))))
        (defn ~unregister-sym [~'name ~'hook]
          ~@(when callback
              `((~callback :unregister ~'name)))
          (swap! ~hooks-sym update ~'name (fn [hooks#]
                                            (remove #{~'hook} hooks#))))
        (defn ~registered-sym []
          (deref ~hooks-sym))
        (defn ~reset-sym []
          (reset! ~hooks-sym (sorted-map)))
        (defn ~process-sym [~'name ~'args]
          (let [~'hooks (get (~registered-sym) ~'name)]
            (reduce (fn [~'args-out ~'hook]
                      (~'hook ~'args-out))
                    ~'args ~'hooks)))))))

(defmacro defhooks
  "Defines a set of hooks under the given name. See defhook* for
   more information."
  ([name]
   (defhooks* name &env))
  ([name callback]
   (defhooks* name callback &env)))

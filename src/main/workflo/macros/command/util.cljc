(ns workflo.macros.command.util
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [workflo.macros.query]
            [workflo.macros.specs.command]
            [workflo.macros.util.symbol]))

;;;; Utilities

(s/fdef bind-query-keys
  :args (s/cat :form-body
               (s/spec :workflo.macros.specs.command/command-form-body)
               :query-keys
               :workflo.macros.query/map-destructuring-keys)
  :ret  :workflo.macros.specs.command/command-form-body)

(defn bind-query-keys
  [form-body query-keys]
  `((~'let [{:keys ~query-keys} ~'query-result]
     ~@form-body)))

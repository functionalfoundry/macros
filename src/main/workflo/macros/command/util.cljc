(ns workflo.macros.command.util
  (:require [clojure.spec :as s]
            [workflo.macros.query]
            [workflo.macros.specs.command :as sc]
            [workflo.macros.specs.parsed-query :as spq]
            [workflo.macros.util.symbol]))

;;;; Utilities

(s/fdef wrap-with-query-bindings
  :args (s/cat :body (s/spec ::sc/command-form-body)
               :query ::spq/query)
  :ret  ::sc/command-form-body)

(defn wrap-with-query-bindings
  [body query]
  `((~'workflo.macros.bind/with-query-bindings
     ~query ~'query-result ~@body)))

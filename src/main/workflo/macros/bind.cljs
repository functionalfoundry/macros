(ns workflo.macros.bind
  (:require-macros [workflo.macros.bind :refer [with-query-bindings]])
  (:require [clojure.spec]
            [workflo.macros.specs.query]))

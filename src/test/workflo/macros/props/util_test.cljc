(ns workflo.macros.props.util-test
  (:require #?(:cljs [cljs.spec.test :refer [check-var]]
               :clj  [clojure.spec.test :refer [check-var]])
            [workflo.macros.props.util]))

(clojure.spec.test/run-tests 'workflo.macros.props.util)

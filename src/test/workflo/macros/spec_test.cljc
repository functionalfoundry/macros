(ns workflo.macros.spec-test
  (:require #?(:cljs [cljs.spec.test :refer-macros [run-all-tests]]
               :clj  [clojure.spec.test :refer [run-all-tests]])))

(run-all-tests)

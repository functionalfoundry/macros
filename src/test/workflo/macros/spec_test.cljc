(ns workflo.macros.spec-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.test :as st]
               :clj  [clojure.spec.test :as st])
            [workflo.macros.entity]
            [workflo.macros.command]
            [workflo.macros.command.util]
            [workflo.macros.jscomponents]
            [workflo.macros.query]
            [workflo.macros.query.bind]
            [workflo.macros.query.om-next]
            [workflo.macros.query.util]
            [workflo.macros.util.form]
            [workflo.macros.util.string]
            [workflo.macros.util.symbol]
            [workflo.macros.screen]
            [workflo.macros.view]))

#?(:cljs (deftest test-specs
           (doseq [v (s/speced-vars)]
             (println "  Testing" v)
             (when-not
                 (some #{v} [#'workflo.macros.query/parse-subquery
                             #'workflo.macros.query/parse
                             #'workflo.macros.util.form/forms-map])
               (let [result (st/check-var v :num-tests 10 :max-size 10)]
                 (println "  >" result)
                 (and (is (map? result))
                      (is (true? (:result result)))))))))

#?(:clj (deftest test-specs
          (doseq [s (st/testable-syms)]
            (println "  Testing" s)
            (let [result (first (st/test s {:clojure.spec.test.check/opts
                                            {:num-tests 10}}))]
              (and (is (map? result))
                   (is (true? (:result result))))))))

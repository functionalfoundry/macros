(ns workflo.macros.spec-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [clojure.spec :as s]
            [clojure.spec.test :as st]
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

(def check-opts
  {:clojure.spec.test.check/opts {:num-tests 10
                                  :max-size 10}})

(defn workflo-sym? [sym]
  (string/starts-with? (str sym) "workflo.macros."))

;; #?(:cljs (deftest test-specs
;;            (doseq [sym (st/checkable-syms)]
;;              (println "  Testing" sym)
;;              (when-not
;;                  (some #{sym}
;;                        ['workflo.macros.query/parse-subquery
;;                         'workflo.macros.query/parse
;;                         'workflo.macros.query.om-next/query
;;                         'workflo.macros.query.om-next/property-query
;;                         'workflo.macros.util.form/forms-map])
;;                (let [result (st/check sym check-opts)]
;;                  (println "  >" result)
;;                  (and (is (map? result))
;;                       (is (true? (:result result)))))))))

#?(:clj (deftest test-specs
          (doseq [s (->> (st/checkable-syms)
                         (filter workflo-sym?))]
            (println "  Testing" s)
            (let [result (first (st/check s check-opts))]
              (and (is (map? result))
                   (is (-> result :clojure.spec.test.check/ret))
                   (is (-> result :clojure.spec.test.check/ret
                           :result true?)))))))

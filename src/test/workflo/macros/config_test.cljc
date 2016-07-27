(ns workflo.macros.config-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [workflo.macros.config
                      :refer-macros [defconfig]]
               :clj  [workflo.macros.config
                      :refer [defconfig]])))

(deftest defconfig-defines-all-expected-functions
  (defconfig view {})
  (and (is (fn? configure-views!))
       (is (fn? get-view-config))))

(deftest defconfig-works-as-expected
  (defconfig item {:foo :default-foo
                   :bar "Default bar"
                   :baz nil})
  (and (is (= {:foo :default-foo
               :bar "Default bar"
               :baz nil}
              (get-item-config)))
       (is (= :default-foo (get-item-config :foo)))
       (is (= "Default bar" (get-item-config :bar)))
       (is (= nil (get-item-config :baz)))
       (is (= nil (get-item-config :ruux)))
       (do
         (configure-items! {:foo :other-foo})
         (and (is (= :other-foo (get-item-config :foo)))
              (is (= "Default bar" (get-item-config :bar)))
              (is (= nil (get-item-config :baz)))
              (is (= nil (get-item-config :ruux)))))))

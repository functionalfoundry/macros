(ns workflo.macros.command-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [workflo.macros.command :as c
                      :refer-macros [defcommand]]
               :clj  [workflo.macros.command :as c
                      :refer [defcommand]])))

(deftest minimal-defcommand
  (is (= (macroexpand-1 '(defcommand user/create [(s/spec map?)]
                           (:result)))
         '())))

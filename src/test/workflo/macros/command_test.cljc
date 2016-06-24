(ns workflo.macros.command-test
  (:require [cljs.test :refer-macros [deftest is]]
            #?(:cljs [workflo.macros.command :as c
                      :refer-macros [defcommand]]
               :clj  [workflo.macros.command :as c
                      :refer [defcommand]])))

(ns workflo.macros.permission-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.permission :as p :refer [defpermission]]))

(deftest simple-defpermission
  (is (= '(do
            (def do-something
              {:name :do-something
               :title "Able to do something"
               :description "A good description"})
            (workflo.macros.permission/register-permission!
             'do-something pod/do-something))
         (macroexpand '(workflo.macros.permission/defpermission do-something
                         (title "Able to do something")
                         (description "A good description"))))))

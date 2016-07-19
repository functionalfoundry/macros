(ns workflo.macros.screen-test
  (:require [cljs.test :refer-macros [deftest is]]
            [workflo.macros.screen :as screen
             :refer-macros [defscreen]]))

(deftest minimal-defscreen
  (is (= '(do
            (def users-name 'users)
            (def users-url
              {:string "screens/users"
               :segments ["screens" "users"]})
            (def users-navigation {:title "Users"})
            (def users-layout {:content user})
            (def users-definition
              {:name workflo.macros.screen-test/users-name
               :url workflo.macros.screen-test/users-url,
               :navigation workflo.macros.screen-test/users-navigation,
               :layout workflo.macros.screen-test/users-layout})
            (workflo.macros.screen/register-screen!
             'users workflo.macros.screen-test/users-definition)))
      (macroexpand-1
       '(defscreen users
          (url "screens/users")
          (navigation
           (title "Users"))
          (layout
           {:content user})))))

(deftest defscreen-with-description-navigation-and-layout
  (is (= '(do
            (def users-name 'users)
            (def users-description "Displays all users")
            (def users-url
              {:string "screens/users"
               :segments ["screens" "users"]})
            (def users-navigation {:title "Users"})
            (def users-layout {:content user})
            (def users-definition
              {:name workflo.macros.screen-test/users-name
               :description workflo.macros.screen-test/users-description
               :url workflo.macros.screen-test/users-url
               :navigation workflo.macros.screen-test/users-navigation
               :layout workflo.macros.screen-test/users-layout})
            (workflo.macros.screen/register-screen!
             'users workflo.macros.screen-test/users-definition))
         (macroexpand-1
          '(defscreen users
             "Displays all users"
             (url "screens/users")
             (navigation
               (title "Users"))
             (layout
               {:content user}))))))

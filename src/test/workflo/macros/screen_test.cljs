(ns workflo.macros.screen-test
  (:require [cljs.test :refer [deftest is]]
            [workflo.macros.screen :as screen :refer [defscreen]]))

(deftest minimal-defscreen
  (is (= '(do
            (def users-name 'users)
            (def users-url
              {:string "screens/users"
               :segments ["screens" "users"]})
            (def users-forms
              {:navigation {:title "Users"}})
            (def users-sections {:content user})
            (def users-definition
              {:name workflo.macros.screen-test/users-name
               :url workflo.macros.screen-test/users-url,
               :forms workflo.macros.screen-test/users-forms,
               :sections workflo.macros.screen-test/users-sections})
            (workflo.macros.screen/register-screen!
             'users workflo.macros.screen-test/users-definition)))
      (macroexpand-1
       '(defscreen users
          (url "screens/users")
          (navigation
           {:title "Users"})
          (sections
           {:content user})))))

(deftest defscreen-with-description-forms-and-sections
  (is (= '(do
            (def users-name 'users)
            (def users-description "Displays all users")
            (def users-url
              {:string "screens/users"
               :segments ["screens" "users"]})
            (def users-forms
              {:navigation {:title "Users"}})
            (def users-sections {:content user})
            (def users-definition
              {:name workflo.macros.screen-test/users-name
               :description workflo.macros.screen-test/users-description
               :url workflo.macros.screen-test/users-url
               :forms workflo.macros.screen-test/users-forms
               :sections workflo.macros.screen-test/users-sections})
            (workflo.macros.screen/register-screen!
             'users workflo.macros.screen-test/users-definition))
         (macroexpand-1
          '(defscreen users
             "Displays all users"
             (url "screens/users")
             (navigation
               {:title "Users"})
             (sections
               {:content user}))))))

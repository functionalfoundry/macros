(ns workflo.macros.util.string-test
  (:require [clojure.test :refer [deftest is]]
            [workflo.macros.util.string :refer [camel->kebab]]))

(deftest camel->kebab-works []
  (and (is (= "user" (camel->kebab "User")))
       (is (= "user-profile" (camel->kebab "UserProfile")))
       (is (= "user-profile-title" (camel->kebab "UserProfileTitle")))))

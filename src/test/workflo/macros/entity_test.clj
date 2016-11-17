(ns workflo.macros.entity-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.entity :as e :refer [defentity]]))

(deftest minimal-defentity
  (is (= '(do
            (def macros-user-name 'macros/user)
            (def macros-user-spec map?)
            (def macros-user-definition
              {:name pod/macros-user-name
               :spec pod/macros-user-spec})
            (workflo.macros.entity/register-entity!
             'macros/user pod/macros-user-definition))
         (macroexpand-1 `(defentity macros/user
                           (~'spec ~'map?))))))

(deftest fully-qualified-entity-name
  (is (= '(do
            (def my-ui-app-name 'my.ui/app)
            (def my-ui-app-spec map?)
            (def my-ui-app-definition
              {:name pod/my-ui-app-name
               :spec pod/my-ui-app-spec})
            (workflo.macros.entity/register-entity!
             'my.ui/app pod/my-ui-app-definition))
         (macroexpand-1 `(defentity my.ui/app
                           (~'spec ~'map?))))))

(deftest defentity-with-auth
  (is (= '(do
            (def macros-user-name 'macros/user)
            (defn macros-user-auth
              [{:keys [foo bar]}]
              (println foo bar))
            (def macros-user-auth-query
              '[{:name foo :type :property}
                {:name bar :type :property}])
            (def macros-user-spec map?)
            (def macros-user-definition
              {:name pod/macros-user-name
               :auth pod/macros-user-auth
               :auth-query pod/macros-user-auth-query
               :spec pod/macros-user-spec})
            (workflo.macros.entity/register-entity!
             'macros/user pod/macros-user-definition))
         (macroexpand-1 `(defentity macros/user
                           ~'(auth [foo bar]
                               (println foo bar))
                           ~'(spec map?))))))

(ns workflo.macros.command-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.command :as c :refer [defcommand]]))

(deftest minimal-defcommand
  (is (= '(do
            (defn user-create-emit
              [query-result data]
              (:foo :bar))
            (def user-create-spec
              vector?)
            (def user-create-definition
              {:spec pod/user-create-spec
               :emit pod/user-create-emit})
            (workflo.macros.command/register-command!
             'user/create pod/user-create-definition))
         (macroexpand-1 `(defcommand user/create
                           (~'spec ~'vector?)
                           (~'emit (:foo :bar)))))))

(deftest fully-qualified-command-name
  (is (= '(do
            (defn my-user-create-emit
              [query-result data]
              (:foo :bar))
            (def my-user-create-spec
              vector?)
            (def my-user-create-definition
              {:spec pod/my-user-create-spec
               :emit pod/my-user-create-emit})
            (workflo.macros.command/register-command!
             'my.user/create pod/my-user-create-definition))
         (macroexpand-1 `(defcommand my.user/create
                           (~'spec ~'vector?)
                           (~'emit (:foo :bar)))))))

(deftest defcommand-with-hints
  (is (= '(do
            (defn my-user-create-emit
              [query-result data]
              (:foo :bar))
            (def my-user-create-hints
              [:bar :baz])
            (def my-user-create-spec
              vector?)
            (def my-user-create-definition
              {:spec pod/my-user-create-spec
               :hints pod/my-user-create-hints
               :emit pod/my-user-create-emit})
            (workflo.macros.command/register-command!
             'my.user/create pod/my-user-create-definition))
         (macroexpand-1 `(defcommand my.user/create
                           (~'hints [:bar :baz])
                           (~'spec ~'vector?)
                           (~'emit (:foo :bar)))))))

(deftest defcommand-with-query
  (is (= '(do
            (defn user-update-emit
              [query-result data]
              (workflo.macros.bind/with-query-bindings
                [{:name user/name :type :property}
                 {:name user/email :type :property}]
                query-result
                {:some :data}))
            (def user-update-query
              '[{:name user/name :type :property}
                {:name user/email :type :property}])
            (def user-update-spec
              vector?)
            (def user-update-definition
              {:spec pod/user-update-spec
               :query pod/user-update-query
               :emit pod/user-update-emit})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update
                           (~'spec ~'vector?)
                           (~'query ~'[user [name email]])
                           (~'emit {:some :data}))))))

(deftest defcommand-with-forms
  (is (= '(do
            (defn user-update-foo
              [query-result data]
              [:bar])
            (defn user-update-emit
              [query-result data]
              {:emit :result})
            (def user-update-spec
              vector?)
            (def user-update-definition
              {:foo pod/user-update-foo
               :emit pod/user-update-emit
               :spec pod/user-update-spec})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update
                           (~'spec ~'vector?)
                           (~'foo [:bar])
                           (~'emit {:emit :result}))))))

(deftest defcommand-with-query-and-forms
  (is (= '(do
            (defn user-create-foo
              [query-result data]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                [:bar]))
            (defn user-create-emit
              [query-result data]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                :result))
            (def user-create-query
              '[{:name db/id :type :property}])
            (def user-create-spec
              map?)
            (def user-create-definition
              {:foo pod/user-create-foo
               :emit pod/user-create-emit
               :query pod/user-create-query
               :spec pod/user-create-spec})
            (workflo.macros.command/register-command!
             'user/create pod/user-create-definition))
         (macroexpand-1 `(defcommand user/create
                           (~'spec ~'map?)
                           (~'query ~'[db [id]])
                           (~'foo [:bar])
                           (~'emit :result))))))

(deftest defcommand-with-query-and-auth
  (is (= '(do
            (defn user-update-emit
              [query-result data]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                :result))
            (def user-update-query
              '[{:name db/id :type :property}])
            (defn user-update-auth
              [query-result auth-query-result]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                :foo))
            (def user-update-definition
              {:emit pod/user-update-emit
               :query pod/user-update-query
               :auth pod/user-update-auth})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update
                           (~'query ~'[db [id]])
                           (~'auth :foo)
                           (~'emit :result))))))

(deftest defcommand-with-query-and-auth-and-auth-query
  (is (= '(do
            (defn user-update-emit
              [query-result data]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                :result))
            (def user-update-query
              '[{:name db/id :type :property}])
            (def user-update-auth-query
              '[{:name foo/bar :type :property}])
            (defn user-update-auth
              [query-result auth-query-result]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                (workflo.macros.bind/with-query-bindings
                  [{:name foo/bar :type :property}]
                  auth-query-result
                  :foo)))
            (def user-update-definition
              {:emit pod/user-update-emit
               :query pod/user-update-query
               :auth-query pod/user-update-auth-query
               :auth pod/user-update-auth})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update
                           (~'query ~'[db [id]])
                           (~'auth-query ~'[foo [bar]])
                           (~'auth :foo)
                           (~'emit :result))))))

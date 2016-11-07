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
                           (~'query ~'[user [name email]])
                           (~'spec ~'vector?)
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
                           (~'query ~'[db [id]])
                           (~'spec ~'map?)
                           (~'foo [:bar])
                           (~'emit :result))))))

;;;; Exercise run-command!

;;; Define a spec for the command data

(s/def ::user-name string?)
(s/def ::user-email string?)
(s/def ::user-create-data
  (s/keys :req-un [::user-name ::user-email]))

;;; Define example query and run emits

(defn example-query
  [query _]
  {:db/id 15})

(defn example-process-emit
  [data _]
  data)

(defcommand user/create
  (query [db [id]])
  (spec ::user-create-data)
  (emit
   {:cache {:db-id id}}))

(c/configure-commands!
  {:query example-query
   :process-emit example-process-emit})

(c/run-command! 'user/create {:user-name "Jeff"
                              :user-email "jeff@jeff.org"})

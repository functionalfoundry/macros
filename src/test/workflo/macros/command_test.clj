(ns workflo.macros.command-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.command :refer [defcommand]]))

(deftest minimal-defcommand
  (is (= '(do
            (defn user-create-implementation
              [query-result data]
              :foo :bar)
            (def user-create-definition
              {:implementation pod/user-create-implementation}))
         (macroexpand-1 `(defcommand user/create [vector?]
                           ~'(:foo :bar))))))

(deftest defcommand-with-cache-query
  (is (= '(do
            (defn user-update-implementation
              [query-result data]
              (let [{:keys [user/name user/email]} query-result]
                {:some :data}))
            (def user-update-definition
              {:implementation pod/user-update-implementation}))
         (macroexpand-1 `(defcommand user/update [~'[user [name email]]
                                                  vector?]
                           ({:some :data}))))))

(deftest defcommand-with-forms
  (is (= '(do
            (defn user-update-workflows
              [query-result data]
              [:foo])
            (defn user-update-lifecycles
              [query-result data]
              [:bar])
            (defn user-update-implementation
              [query-result data]
              {:implementation :result})
            (def user-update-definition
              {:implementation pod/user-update-implementation
               :lifecycles pod/user-update-lifecycles
               :workflows pod/user-update-workflows}))
         (macroexpand-1 `(defcommand user/update [vector?]
                           (~'workflows [:foo])
                           (~'lifecycles [:bar])
                           ({:implementation :result}))))))

(deftest defcommand-with-cache-query-and-forms
  (is (= '(do
            (defn user-create-workflows
              [query-result data]
              (let [{:keys [db/id]} query-result]
                [:foo]))
            (defn user-create-lifecycles
              [query-result data]
              (let [{:keys [db/id]} query-result]
                [:bar]))
            (defn user-create-implementation
              [query-result data]
              (let [{:keys [db/id]} query-result]
                :result))
            (def user-create-definition
              {:implementation pod/user-create-implementation
               :lifecycles pod/user-create-lifecycles
               :workflows pod/user-create-workflows}))
         (macroexpand-1 `(defcommand user/create [~'[db [id]] map?]
                           (~'workflows [:foo])
                           (~'lifecycles [:bar])
                           (:result))))))

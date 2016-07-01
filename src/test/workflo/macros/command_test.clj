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
            (def user-update-cache-query
              [{:name user/name :type :property}
               {:name user/email :type :property}])
            (def user-update-definition
              {:cache-query pod/user-update-cache-query
               :implementation pod/user-update-implementation}))
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
              {:workflows pod/user-update-workflows
               :lifecycles pod/user-update-lifecycles
               :implementation pod/user-update-implementation}))
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
            (def user-create-cache-query
              [{:name db/id :type :property}])
            (def user-create-definition
              {:workflows pod/user-create-workflows
               :lifecycles pod/user-create-lifecycles
               :implementation pod/user-create-implementation
               :cache-query pod/user-create-cache-query}))
         (macroexpand-1 `(defcommand user/create [~'[db [id]] map?]
                           (~'workflows [:foo])
                           (~'lifecycles [:bar])
                           (:result))))))

(ns workflo.macros.command-test
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [workflo.macros.command :as c
                      :refer-macros [defcommand]]
               :clj  [workflo.macros.command :as c
                      :refer [defcommand]])))

(defcommand user/create [(s/spec vector?)]
  (:foo :bar))

(deftest minimal-defcommand
  (is (= (macroexpand-1 `(defcommand user/create [~(s/spec vector?)]
                           (:foo :bar)))
         '(do
            (defn user-create-implementation
              [query-result data]
              :foo :bar)
            (def user-create-definition
              {:implementation
               workflo.macros.command/user-create-implementation})))))

(deftest defcommand-with-cache-query
  (is (= (macroexpand-1 `(defcommand user/update [[user [name email]]
                                                  ~(s/spec vector?)]
                           ({:some :data})))
         '(do
            (defn user-update-implementation
              [query-result data]
              (let [{:keys [user/name user/email]} query-result]
                {:some :data}))
            (def user-update-definition
              {:implementation
               workflo.macros.command/user-update-implementation})))))

(deftest defcommand-with-forms
  (is (= (macroexpand-1 `(defcommand user/update [~(s/spec vector?)]
                           (~'workflows [:foo])
                           (~'lifecycles [:bar])
                           ({:implementation :result})))
         '(do
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
              {:implementation
               workflo.macros.command/user-update-implementation
               :lifecycles
               workflo.macros.command/user-update-lifecycles
               :workflows
               workflo.macros.command/user-update-workflows})))))

(deftest defcommand-with-cache-query-and-forms
  (is (= (macroexpand-1 `(defcommand user/create [[db [id]]
                                                  ~(s/spec map?)]
                           (~'workflows [:foo])
                           (~'lifecycles [:bar])
                           (:result)))
         '(do
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
              {:implementation
               workflo.macros.command/user-create-implementation
               :lifecycles
               workflo.macros.command/user-create-lifecycles
               :workflows
               workflo.macros.command/user-create-workflows})))))

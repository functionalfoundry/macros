(ns workflo.macros.command-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.command :as c :refer [defcommand]]))

(deftest minimal-defcommand
  (is (= '(do
            (defn user-create-implementation
              [query-result data]
              (:foo :bar))
            (def user-create-data-spec
              vector?)
            (def user-create-definition
              {:data-spec pod/user-create-data-spec
               :implementation pod/user-create-implementation})
            (workflo.macros.command/register-command!
             'user/create pod/user-create-definition))
         (macroexpand-1 `(defcommand user/create [~'vector?]
                           ~'(:foo :bar))))))

(deftest defcommand-with-cache-query
  (is (= '(do
            (defn user-update-implementation
              [query-result data]
              (let [{:keys [user/name user/email]} query-result]
                {:some :data}))
            (def user-update-cache-query
              '[{:name user/name :type :property}
                {:name user/email :type :property}])
            (def user-update-data-spec
              vector?)
            (def user-update-definition
              {:data-spec pod/user-update-data-spec
               :cache-query pod/user-update-cache-query
               :implementation pod/user-update-implementation})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update [~'[user [name email]]
                                                  ~'vector?]
                           {:some :data})))))

(deftest defcommand-with-forms
  (is (= '(do
            (defn user-update-foo
              [query-result data]
              [:bar])
            (defn user-update-implementation
              [query-result data]
              {:implementation :result})
            (def user-update-data-spec
              vector?)
            (def user-update-definition
              {:foo pod/user-update-foo
               :implementation pod/user-update-implementation
               :data-spec pod/user-update-data-spec})
            (workflo.macros.command/register-command!
             'user/update pod/user-update-definition))
         (macroexpand-1 `(defcommand user/update [~'vector?]
                           (~'foo [:bar])
                           {:implementation :result})))))

(deftest defcommand-with-cache-query-and-forms
  (is (= '(do
            (defn user-create-foo
              [query-result data]
              (let [{:keys [db/id]} query-result]
                [:bar]))
            (defn user-create-implementation
              [query-result data]
              (let [{:keys [db/id]} query-result]
                :result))
            (def user-create-cache-query
              '[{:name db/id :type :property}])
            (def user-create-data-spec
              map?)
            (def user-create-definition
              {:foo pod/user-create-foo
               :implementation pod/user-create-implementation
               :cache-query pod/user-create-cache-query
               :data-spec pod/user-create-data-spec})
            (workflo.macros.command/register-command!
             'user/create pod/user-create-definition))
         (macroexpand-1 `(defcommand user/create [~'[db [id]] ~'map?]
                           (~'foo [:bar])
                           :result)))))

;;;; Exercise run-command

;;; Define a spec for the command data

(s/def ::user-name string?)
(s/def ::user-email string?)
(s/def ::user-create-data
  (s/keys :req-un [::user-name ::user-email]))

;;; Define example query and run implementations

(defn example-query
  [query]
  {:db/id 15})

(defn example-process-result
  [data]
  data)

(defcommand user/create
  [[db [id]] ::user-create-data]
  {:cache {:db-id id}})

(c/configure! {:query example-query
               :process-result example-process-result})

(c/run-command 'user/create {:user-name "Jeff"
                             :user-email "jeff@jeff.org"})

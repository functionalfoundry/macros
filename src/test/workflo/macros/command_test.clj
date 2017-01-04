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

(deftest defcommand-with-query-and-auth-and-generated-auth-query
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
              (workflo.macros.command/conform-and-parse (gen-query)))
            (defn user-update-auth
              [query-result auth-query-result]
              (workflo.macros.bind/with-query-bindings
                [{:name db/id :type :property}]
                query-result
                (workflo.macros.bind/with-query-bindings
                  (workflo.macros.command/conform-and-parse (gen-query))
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
                           (~'auth-query ~'(gen-query))
                           (~'auth :foo)
                           (~'emit :result))))))

;;;; Exercise run-command!

(deftest exercise-run-command-with-authorization
  ;; Use a user-counters atom for testing
  (def user-counters
    (atom [{:user/name :jeff :user/counter 0}
           {:user/name :joe :user/counter 0}]))

  ;; Example query, auth-query and process-emit hooks
  (defn example-query-hook
    [query context]
    ;; Verify the query is what we'd expect
    (is (= [{:name 'user
             :type :join
             :join-source {:name 'user :type :property}
             :join-target [{:name 'user/name :type :property :alias 'user-name}
                           {:name 'user/counter :type :property}]
             :parameters {'user/name (:user context)}}]
           query))
    ;; Simulate running the query
    {:user (first (filter (fn [user-counter]
                            (= (:user/name user-counter)
                               (:user context)))
                          @user-counters))})

  (defn example-auth-query-hook
    [query context]
    ;; Verify the auth query is what we'd expect
    (is (= '[{:name permissions
              :type :join
              :join-source {:name permissions :type :property}
              :join-target [{:name name :type :property}]}]
           query))
    ;; Simulate running the auth query, returning permissions only for jeff
    (if (= :jeff (:user context))
      {:permissions [{:name :update}]}
      {:permissions []}))

  (defn example-process-emit-hook
    [emit-output context]
    (swap! user-counters
           (fn [user-counters]
             (mapv (fn [user-counter]
                     (cond-> user-counter
                       (= (:user/name emit-output)
                          (:user/name user-counter))
                       (assoc :user/counter (:user/counter emit-output))))
                   user-counters))))


  (c/configure-commands!
   {:query example-query-hook
    :auth-query example-auth-query-hook
    :process-emit example-process-emit-hook})

  ;; Define a spec for the command data
  (s/def :update-user-counter/user keyword?)
  (s/def ::update-user-counter-data
    (s/keys :req [:update-user-counter/user]))

  (defcommand update-user-counter
    (spec ::update-user-counter-data)
    (query
      [({user [user [name :as user-name counter]]}
        {user/name ?user})])
    (auth-query
      [{permissions [name]}])
    (auth
      (is (or (= {:user/name :jeff :user/counter 0} user)
              (= {:user/name :joe :user/counter 0} user)))
      (if (= :jeff user-name)
        (is (= [{:name :update}] permissions))
        (is (= [] permissions)))
      ;; Only users for which permissions are available are authorized
      ;; to update counters
      (and user (seq permissions)))
    (emit
      {:user/name user-name
       :user/counter (inc counter)}))

  ;; Run the command as an authorized user
  (c/run-command! 'update-user-counter
                  {:update-user-counter/user :jeff}
                  {:user :jeff})

  ;; Verify that the user's counter has changed
  (is (= [{:user/name :jeff :user/counter 1}
          {:user/name :joe :user/counter 0}]
         @user-counters))

  ;; Verify that running the command as an unauthorized user
  ;; throws an exception
  (is (thrown? Exception
               (c/run-command! 'update-user-counter
                               {:update-user-counter/user :joe}
                               {:user :joe})))

  ;; Verify that after the exception, the data hasn't changed,
  ;; meaning that the command wasn't executed
  (is (= [{:user/name :jeff :user/counter 1}
          {:user/name :joe :user/counter 0}]
         @user-counters)))

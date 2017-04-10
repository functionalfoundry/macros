(ns workflo.macros.command-run-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.command :as c :refer [defcommand]]))

;; Use a user-counters atom for testing
(def user-counters
  (atom [{:user/name :jeff :user/counter 0}
         {:user/name :joe :user/counter 0}]))

;; Example query, auth-query and process-emit hooks
(defn example-query-hook
  [{:keys [query context] :as data}]
  ;; Verify the query is what we'd expect
  (is (= [{:name 'user
           :type :join
           :join-source {:name 'user :type :property}
           :join-target [{:name 'user/name :type :property :alias 'user-name}
                         {:name 'user/counter :type :property}]
           :parameters {'user/name (:user context)}}]
         query))
  ;; Simulate running the query
  (assoc data :query-result {:user (first (filter (fn [user-counter]
                                                    (= (:user/name user-counter)
                                                       (:user context)))
                                                  @user-counters))}))

(defn example-auth-query-hook
  [{:keys [query context] :as data}]
  ;; Verify the auth query is what we'd expect
  (is (= '[{:name permissions
            :type :join
            :join-source {:name permissions :type :property}
            :join-target [{:name name :type :property}]}]
         query))
  ;; Simulate running the auth query, returning permissions only for jeff
  (assoc data :query-result (if (= :jeff (:user context))
                              {:permissions [{:name :update}]}
                              {:permissions []})))

(defn example-process-emit-hook
  [{:keys [output context]}]
  (swap! user-counters
         (fn [user-counters]
           (mapv (fn [user-counter]
                   (cond-> user-counter
                     (= (:user/name output)
                        (:user/name user-counter))
                     (assoc :user/counter (:user/counter output))))
                 user-counters))))

(deftest exercise-run-command-with-authorization
  (c/register-command-hook! :query example-query-hook)
  (c/register-command-hook! :auth-query example-auth-query-hook)
  (c/register-command-hook! :process-emit example-process-emit-hook)

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
  ;; throws an exception in Clojure but not in ClojureScript (where
  ;; authorization is disabled)
  #?(:cljs
     (c/run-command! 'update-user-counter
                     {:update-user-counter/user :joe}
                     {:user :joe})
     :clj
     (is (thrown? #?(:cljs js/Error :clj Exception)
                  (c/run-command! 'update-user-counter
                                  {:update-user-counter/user :joe}
                                  {:user :joe}))))

  ;; Verify that, in Clojure, the unauthorized command wasn't executed,
  ;; whereas it was executed in ClojureScript
  #?(:cljs
     (is (= [{:user/name :jeff :user/counter 1}
             {:user/name :joe :user/counter 1}]
            @user-counters))
     :clj
     (is (= [{:user/name :jeff :user/counter 1}
             {:user/name :joe :user/counter 0}]
            @user-counters))))

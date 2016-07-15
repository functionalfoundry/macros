(ns workflo.macros.entity-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [workflo.macros.entity :as e :refer [defentity]]))

(deftest minimal-defentity
  (is (= '(do
            (def macros-user-schema map?)
            (def macros-user-definition
              {:schema pod/macros-user-schema}))
         (macroexpand-1 `(defentity macros/user
                           (~'schema ~'map?))))))

(deftest defentity-with-validation
  (is (= '(do
            (def macros-user-validation
              (s/keys :req [:foo/bar]
                      :opt [:bar/baz]))
            (def macros-user-schema map?)
            (def macros-user-definition
              {:schema pod/macros-user-schema
               :validation pod/macros-user-validation}))
         (macroexpand-1 `(defentity macros/user
                           (~'validation
                             (~'s/keys :req [:foo/bar]
                                       :opt [:bar/baz]))
                           (~'schema ~'map?))))))

(deftest defentity-with-auth
  (is (= '(do
            (defn macros-user-auth
              [{:keys [foo bar]}]
              (println foo bar))
            (def macros-user-auth-query
              '[{:name foo :type :property}
                {:name bar :type :property}])
            (def macros-user-schema map?)
            (def macros-user-definition
              {:auth pod/macros-user-auth
               :auth-query pod/macros-user-auth-query
               :schema pod/macros-user-schema}))
         (macroexpand-1 `(defentity macros/user
                           ~'(auth [foo bar]
                             (println foo bar))
                           ~'(schema map?))))))

(deftest defentity-with-auth-and-validation
  (is (= '(do
            (defn macros-user-auth
              [{:keys [foo bar]}]
              (println foo bar))
            (def macros-user-auth-query
              '[{:name foo :type :property}
                {:name bar :type :property}])
            (def macros-user-validation
              (s/keys :req-un [::foo ::bar]))
            (def macros-user-schema map?)
            (def macros-user-definition
              {:auth pod/macros-user-auth
               :auth-query pod/macros-user-auth-query
               :validation pod/macros-user-validation
               :schema pod/macros-user-schema}))
         (macroexpand-1 `(defentity macros/user
                           ~'(auth [foo bar]
                                   (println foo bar))
                           ~'(validation
                               (s/keys :req-un [::foo ::bar]))
                           ~'(schema map?))))))

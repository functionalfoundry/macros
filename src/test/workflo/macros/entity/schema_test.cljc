(ns workflo.macros.entity.schema-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is]]
            [workflo.macros.entity :refer [registered-entities resolve-entity]]
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.entity.test-entities]
            [workflo.macros.specs.types :as types]))

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= {:url/selected-user [:long]}
                (schema/entity-schema entity))))))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= {:ui/search-text [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-keys-spec
  (let [entity (resolve-entity 'user)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]
                 :user/address [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]
                 :user/address [:string]}
                (schema/entity-schema entity))))))

;;;; Matching schemas

(deftest matching-entity-schemas
  (is (= {:url/selected-user [:long]
          :ui/search-text [:string]
          :ui/search-text-with-extended-spec [:string]}
         (schema/matching-entity-schemas (registered-entities)
                                         #"^(url|ui)/.*"))))

;;;; Keys, required keys, optional keys

(deftest all-keys
  (are [x y] (= x (-> y resolve-entity schema/keys))
    {} 'url/selected-user
    {} 'ui/search-text
    {} 'ui/search-text-with-extended-spec
    {:required [:base/id
                :user/name
                :user/email
                :user/role]
     :optional [:user/bio]} 'user
    {:required [:base/id
                :user/name
                :user/email
                :user/role]
     :optional [:user/bio
                :user/address]} 'user-with-extended-spec))

(deftest required-keys
  (are [x y] (= x (-> y resolve-entity schema/required-keys))
    [] 'url/selected-user
    [] 'ui/search-text
    [] 'ui/search-text-with-extended-spec
    [:base/id
     :user/name
     :user/email
     :user/role] 'user
    [:base/id
     :user/name
     :user/email
     :user/role] 'user-with-extended-spec))

(deftest optional-keys
  (are [x y] (= x (-> y resolve-entity schema/optional-keys))
    [] 'url/selected-user
    [] 'ui/search-text
    [] 'ui/search-text-with-extended-spec
    [:user/bio] 'user
    [:user/bio
     :user/address] 'user-with-extended-spec))

;;;; Non-persistent keys

(deftest non-persistent-keys
  (are [x y] (= x (-> y resolve-entity schema/non-persistent-keys))
    [] 'url/selected-user
    [] 'ui/search-text
    [] 'ui/search-text-with-extended-spec
    [] 'user
    [:user/address] 'user-with-extended-spec))

;;;; Entity refs

;;; Entities with refs between them

(deftest entity-ref-describe
  (are [desc spec] (= desc (s/describe spec))
    ;; Without options
    '(entity-ref user) (types/entity-ref 'user)
    '(entity-ref comment) (types/entity-ref 'comment)

    ;; With options
    '(entity-ref user :many? true)
    (types/entity-ref 'user :many? true)))

(deftest entity-with-entity-refs
  (let [entity (resolve-entity 'post)]
    (and (is (not (nil? entity)))
         (is (= {:post/author [:ref]
                 :post/text [:string]
                 :post/comments [:ref :many]}
                (schema/entity-schema entity))))))

;;; Entities with a top-level entity-ref spec

(deftest entity-with-entity-ref-spec
  (let [entity (resolve-entity 'current-post)]
    (and (is (not (nil? entity)))
         (is (= {:current-post [:ref]}
                (schema/entity-schema entity))))))

(deftest entity-with-entity-ref-many-spec
  (let [entity (resolve-entity 'previous-posts)]
    (and (is (not (nil? entity)))
         (is (= {:previous-posts [:ref :many]}
                (schema/entity-schema entity))))))

;;;; Entity refs

(deftest entity-refs
  (are [joins entity] (= joins (-> entity resolve-entity
                                   schema/entity-refs))
    {:post/author {:entity 'author}
     :post/comments {:entity 'comment :many? true}} 'post
    {:comment/author {:entity 'author}} 'comment
    {:entity 'post} 'current-post
    {:entity 'post :many? true} 'previous-posts
    nil 'ui/search-text))

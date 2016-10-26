(ns workflo.macros.entity.schema-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is]]
            [workflo.macros.entity :refer [resolve-entity defentity]]
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.specs.types :as types]))

(defentity url/selected-user
  (spec ::types/id))

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= {:url/selected-user [:long]}
                (schema/entity-schema entity))))))

(defentity ui/search-text
  (spec ::types/string))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= {:ui/search-text [:string]}
                (schema/entity-schema entity))))))

(defentity ui/search-text-with-extended-spec
  (spec
   (s/and ::types/string
          #(> (count %) 5))))

(s/def :base/id (s/and ::types/uuid ::types/unique-identity))
(s/def :user/email (s/and ::types/string
                          ::types/unique-value
                          #(> (count %) 5)))
(s/def :user/name ::types/string)
(s/def :user/role (s/and ::types/enum #{:user :admin :owner}))
(s/def :user/bio ::types/string)

(defentity user
  (spec
   (s/keys :req [:base/id :user/name :user/email :user/role]
           :opt [:user/bio])))

(deftest entity-with-keys-spec
  (let [entity (resolve-entity 'user)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(defentity user-with-extended-spec
  (spec
   (s/and (s/keys :req [:base/id :user/name :user/email :user/role]
                  :opt [:user/bio])
          #(> (count (:user/name %)) 5))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:base/id [:uuid :unique-identity]
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

;;;; Matching schemas

(deftest matching-entity-schemas
  (is (= {:url/selected-user [:long]
          :ui/search-text [:string]
          :ui/search-text-with-extended-spec [:string]}
         (schema/matching-entity-schemas #"^(url|ui)/.*"))))

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
     :optional [:user/bio]} 'user-with-extended-spec))

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
    [:user/bio] 'user-with-extended-spec))

;;;; Entity refs

;;; Entities with refs between them

(s/def :post/author (types/entity-ref 'author))
(s/def :post/text ::types/string)
(s/def :post/comments (types/entity-ref 'comment :many? true))

(defentity post
  (spec (s/keys :req [:post/author
                      :post/text]
                :opt [:post/comments])))

(s/def :author/name ::types/string)

(defentity author
  (spec (s/keys :req [:author/name])))

(s/def :comment/author (types/entity-ref 'author))
(s/def :comment/text ::types/string)

(defentity comment
  (spec (s/keys :req [:comment/author
                      :comment/text])))

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

(defentity current-post
  (spec (types/entity-ref 'post)))

(defentity previous-posts
  (spec (types/entity-ref 'post :many? true)))

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

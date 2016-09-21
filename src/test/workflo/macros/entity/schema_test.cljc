(ns workflo.macros.entity.schema-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is]]
            [workflo.macros.entity :refer [resolve-entity defentity]]
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.specs.types]))

(defentity url/selected-user
  (spec :workflo.macros.specs.types/id))

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= {:url/selected-user []}
                (schema/entity-schema entity))))))

(defentity ui/search-text
  (spec :workflo.macros.specs.types/string))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= {:ui/search-text [:string]}
                (schema/entity-schema entity))))))

(defentity ui/search-text-with-extended-spec
  (spec
   (s/and :workflo.macros.specs.types/string
          #(> (count %) 5))))

(s/def :db/id :workflo.macros.specs.types/id)
(s/def :user/email (s/and :workflo.macros.specs.types/string
                          :workflo.macros.specs.types/unique-value
                          #(> (count %) 5)))
(s/def :user/name :workflo.macros.specs.types/string)
(s/def :user/role (s/and :workflo.macros.specs.types/enum
                         #{:user :admin :owner}))
(s/def :user/bio :workflo.macros.specs.types/string)

(defentity user
  (spec
   (s/keys :req [:db/id :user/name :user/email :user/role]
           :opt [:user/bio])))

(deftest entity-with-keys-spec
  (let [entity (resolve-entity 'user)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(defentity user-with-extended-spec
  (spec
   (s/and (s/keys :req [:db/id :user/name :user/email :user/role]
                  :opt [:user/bio])
          #(> (count (:user/name %)) 5))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string :unique-value]
                 :user/name [:string]
                 :user/role [:enum [:admin :user :owner]]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(deftest matching-entity-schemas
  (is (= {:url/selected-user []
          :ui/search-text [:string]
          :ui/search-text-with-extended-spec [:string]}
         (schema/matching-entity-schemas #"^(url|ui)/.*"))))

(deftest all-keys
  (are [x y] (= x (-> y resolve-entity schema/keys))
    {:required [] :optional []} 'url/selected-user
    {:required [] :optional []} 'ui/search-text
    {:required [] :optional []} 'ui/search-text-with-extended-spec
    {:required [:db/id
                :user/name
                :user/email
                :user/role]
     :optional [:user/bio]} 'user
    {:required [:db/id
                :user/name
                :user/email
                :user/role]
     :optional [:user/bio]} 'user-with-extended-spec))

(deftest required-keys
  (are [x y] (= x (-> y resolve-entity schema/required-keys))
    [] 'url/selected-user
    [] 'ui/search-text
    [] 'ui/search-text-with-extended-spec
    [:db/id :user/name :user/email :user/role] 'user
    [:db/id :user/name :user/email :user/role] 'user-with-extended-spec))

(deftest optional-keys
  (are [x y] (= x (-> y resolve-entity schema/optional-keys))
    [] 'url/selected-user
    [] 'ui/search-text
    [] 'ui/search-text-with-extended-spec
    [:user/bio] 'user
    [:user/bio] 'user-with-extended-spec))

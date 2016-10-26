(ns workflo.macros.entity.datomic-schema-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is]]
            [workflo.macros.entity :refer [resolve-entity defentity]]
            [workflo.macros.entity.datomic :as datomic-schema]
            [workflo.macros.specs.types :as types]
            [workflo.macros.util.misc :refer [val-after]]))

;;;; Helpers

(defn datomic-attrs [schema]
  (into #{}
        (map (fn [attr-schema]
               (if (vector? attr-schema)
                 (val-after attr-schema :db/ident)
                 (:db/ident attr-schema))))
        schema))

;;;; Tests

(defentity url/selected-user
  (spec ::types/id))

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= #{:url/selected-user}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(defentity ui/search-text
  (spec ::types/string))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= #{:ui/search-text}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

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
         (is (= #{:base/id :user/email :user/name :user/role :user/bio
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(defentity user-with-extended-spec
  (spec
   (s/and (s/keys :req [:base/id :user/name :user/email :user/role]
                  :opt [:user/bio])
          #(> (count (:user/name %)) 5))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= #{:base/id :user/email :user/name :user/role :user/bio
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= #{:base/id :user/email :user/name :user/role :user/bio
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

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

(deftest entity-with-entity-refs
  (let [entity (resolve-entity 'post)]
    (and (is (not (nil? entity)))
         (is (= #{:post/author :post/text :post/comments}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

;;; Entities with a top-level entity-ref spec

(defentity current-post
  (spec (types/entity-ref 'post)))

(defentity previous-posts
  (spec (types/entity-ref 'post :many? true)))

(deftest entity-with-entity-ref-spec
  (let [entity (resolve-entity 'current-post)]
    (and (is (not (nil? entity)))
         (is (thrown? IllegalArgumentException
                      (-> entity datomic-schema/entity-schema
                          datomic-attrs))))))

(deftest entity-with-entity-ref-many-spec
  (let [entity (resolve-entity 'previous-posts)]
    (and (is (not (nil? entity)))
         (is (thrown? IllegalArgumentException
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))


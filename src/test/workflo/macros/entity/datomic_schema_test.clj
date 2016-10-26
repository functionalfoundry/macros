(ns workflo.macros.entity.datomic-schema-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is]]
            [workflo.macros.entity :refer [resolve-entity defentity]]
            [workflo.macros.entity.datomic :as datomic-schema]
            [workflo.macros.entity.test-entities]
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

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= #{:url/selected-user}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= #{:ui/search-text}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(deftest entity-with-keys-spec
  (let [entity (resolve-entity 'user)]
    (and (is (not (nil? entity)))
         (is (= #{:base/id :user/email :user/name :user/role :user/bio
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= #{:base/id :user/email :user/name
                  :user/role :user/bio :user/address
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= #{:base/id :user/email :user/name
                  :user/role :user/bio :user/address
                  :user.role/user :user.role/admin :user.role/owner}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

;;;; Entity refs

;;; Entities with refs between them

(deftest entity-with-entity-refs
  (let [entity (resolve-entity 'post)]
    (and (is (not (nil? entity)))
         (is (= #{:post/author :post/text :post/comments}
                (-> entity datomic-schema/entity-schema
                    datomic-attrs))))))

;;; Entities with a top-level entity-ref spec

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


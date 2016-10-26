(ns workflo.macros.entity.test-entities
  (:require [clojure.spec :as s]
            [workflo.macros.entity :refer [defentity]]
            [workflo.macros.specs.types :as types]))

(defentity url/selected-user
  (spec ::types/id))

(defentity ui/search-text
  (spec ::types/string))

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

(s/def :user/address ::types/string)

(defentity user-with-extended-spec
  (spec
   (s/and (s/keys :req [:base/id :user/name :user/email :user/role]
                  :opt [:user/bio :user/address])
          #(> (count (:user/name %)) 5))))

;;;; Entities with refs between them

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

;;;; Entities with a top-level entity-ref spec

(defentity current-post
  (spec (types/entity-ref 'post)))

(defentity previous-posts
  (spec (types/entity-ref 'post :many? true)))

(ns workflo.macros.entity.refs-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [are deftest is use-fixtures]]
            [workflo.macros.entity :as e]
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.entity.test-entities]
            [workflo.macros.specs.types :as types]))

(deftest refs-for-an-entity
  (is (nil? (e/entity-refs 'url/selected-user)))
  (is (nil? (e/entity-refs 'ui/search-text)))
  (is (nil? (e/entity-refs 'ui/search-text-with-extended-spec)))
  (is (nil? (e/entity-refs 'user)))
  (is (nil? (e/entity-refs 'user-with-extended-spec)))
  (is (nil? (e/entity-refs 'author)))
  (is (nil? (e/entity-refs 'current-post)))
  (is (nil? (e/entity-refs 'previous-post)))

  (is (= {:post/author {:entity 'author :many? false}
          :post/comments {:entity 'comment :many? true}}
         (e/entity-refs 'post)))

  (is (= {:comment/author {:entity 'author :many? false}}
         (e/entity-refs 'comment))))

(deftest backrefs-for-an-entity
  (is (nil? (e/entity-backrefs 'url/selected-user)))
  (is (nil? (e/entity-backrefs 'ui/search-text)))
  (is (nil? (e/entity-backrefs 'ui/search-text-with-extended-spec)))
  (is (nil? (e/entity-backrefs 'user)))
  (is (nil? (e/entity-backrefs 'user-with-extended-spec)))
  (is (nil? (e/entity-backrefs 'post)))

  (is (= {:post/author {:entity 'post :many? true}
          :comment/author {:entity 'comment :many? true}}
         (e/entity-backrefs 'author)))

  (is (= {:post/comments {:entity 'post :many? true}}
         (e/entity-backrefs 'comment))))

(deftest refs-and-backrefs-before-and-after-entity
  (s/def :foo/bar ::types/string)
  (e/defentity foo (spec (s/keys :req [:foo/bar])))

  (let [refs-before     (e/entity-refs 'foo)
        backrefs-before (e/entity-backrefs 'foo)]

    (s/def :bar/foo (types/entity-ref 'foo))
    (e/defentity bar (spec (s/keys :req [:bar/foo])))
    (e/unregister-entity! 'bar)

    (let [refs-after     (e/entity-refs 'foo)
          backrefs-after (e/entity-backrefs 'foo)]
      (is (= refs-before refs-after))
      (is (= backrefs-before backrefs-after)))))

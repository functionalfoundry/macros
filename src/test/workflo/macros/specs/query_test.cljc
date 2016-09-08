(ns workflo.macros.specs.query-test
  (:require #?(:cljs [cljs.test :refer-macros [are deftest is]]
               :clj  [clojure.test :refer [are deftest is]])
            #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [workflo.macros.specs.query]))

(deftest simple-properties
  (let [spec :workflo.macros.specs.query/simple-property]
    (are [out in] (= out (s/conform spec in))
      'foo 'foo
      'bar 'bar)))

(deftest links
  (let [spec :workflo.macros.specs.query/link]
    (are [out in] (= out (s/conform spec in))
      '[foo _] '[foo _]
      '[bar 1] '[bar 1]
      '[baz :x] '[baz :x])))

(deftest join-property
  (let [spec :workflo.macros.specs.query/join-property]
    (are [out in] (= out (s/conform spec in))
      '[:property-name foo] 'foo
      '[:property-name bar] 'bar
      '[:link [foo _]] '[foo _]
      '[:link [bar 1]] '[bar 1]
      '[:link [baz :x]] '[baz :x])))

(deftest model-join
  (let [spec :workflo.macros.specs.query/model-join]
    (are [out in] (= out (s/conform spec in))
      '{user User} '{user User}
      '{user-list UserList} '{user-list UserList}
      '{[user 1] User} '{[user 1] User}
      '{[current-user _] User} '{[current-user _] User})))

(deftest recursion
  (let [spec :workflo.macros.specs.query/recursion]
    (are [out in] (= out (s/conform spec in))
      [:unlimited '...] '...
      [:limited 1] 1
      [:limited 100] 100)))

(deftest recursive-join
  (let [spec :workflo.macros.specs.query/recursive-join]
    (are [out in] (= out (s/conform spec in))
      '{user [:unlimited ...]} '{user ...}
      '{user-list [:limited 5]} '{user-list 5}
      '{[user 1] [:unlimited ...]} '{[user 1] ...}
      '{[user 1] [:limited 100]} '{[user 1] 100})))

(deftest properties-join
  (let [spec :workflo.macros.specs.query/properties-join]
    (are [out in] (= out (s/conform spec in))
      '{} '{})))

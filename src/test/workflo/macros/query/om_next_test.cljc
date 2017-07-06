(ns workflo.macros.query.om-next-test
  (:require [clojure.test :refer [are deftest]]
            [workflo.macros.query.om-next :as om]))


(deftest disambiguate-queries-without-conflicts
  (are [out in] (= out (om/disambiguate in))
    ;; Identical keywords are merged
    [[:foo :bar :baz]]
    [:foo :foo :bar :bar :baz :baz]

    ;; Queries of joins with identical join sources are combined
    [[{:foo [:bar :baz]}
      {:bar [:baz :ruux]}]]
    [{:foo [:bar]}
     {:foo [:baz]}
     {:bar [:baz]}
     {:bar [:baz :ruux]}]

    ;; Queries of joins with identical join sources are
    ;; combined and disambiguated recursively
    [[{:foo [{:bar [:baz]}]}]]
    [{:foo [{:bar [:baz :baz]}
            {:bar [:baz :baz]}]}]

    ;; Queries of joins with different limited recursions
    ;; combined so that the higher recursion number wins
    [[{:foo 10}]]
    [{:foo 5} {:foo 2} {:foo 10} {:foo 3}]

    ;; Identical parameterized queries are merged
    '[[(:foo {:bar :baz})]]
    '[(:foo {:bar :baz})
      (:foo {:bar :baz})]

    ;; Parameterized queries are merged and combined if their
    ;; parameters are identical
    '[[({:foo [:bar :baz]} {:bar :baz})]]
    '[(:foo {:bar :baz})
      ({:foo [:bar]} {:bar :baz})
      ({:foo [:bar :baz]} {:bar :baz})]))


(deftest disambiguate-queries-with-conflicts
  (are [out in] (= out (om/disambiguate in))
    ;; Identical parameterized queries are split if their
    ;; parameters differ
    '[[(:foo {:bar :baz})]
      [(:foo {:bar :ruux})]]
    '[(:foo {:bar :baz})
      (:foo {:bar :ruux})]

    ;; Parameterized and non-parameterized queries are split
    ;; even if their dispatch keys match
    '[[:foo]
      [(:foo {:bar :baz})]]
    '[:foo (:foo {:bar :baz})]

    ;; Recursive and non-recursive joins are split even if
    ;; their dispatch keys match
    '[[{:foo ...}]
      [{:foo [:bar]}]]
    '[{:foo ...} {:foo [:bar]}]

    ;; Joins with limited recursion and no limited recursion
    ;; are split even if their dispatch keys match
    '[[{:foo 1}]
      [{:foo ...}]
      [{:foo [:bar]}]]
    '[{:foo 1} {:foo ...} {:foo [:bar]}]


    ;; Ident and non-ident queries are split even if their
    ;; dispatch keys match
    [[:foo]
     [[:foo 1]]]
    [:foo
     [:foo 1]]

    ;; Idents with different values are split even if their
    ;; dispatch keys match
    [[[:foo 1]]
     [[:foo 2]]]
    [[:foo 1]
     [:foo 2]]

    ;; Joins with a conflict in their subqueries are split
    '[[{:foo [(:bar {:a :b}) :baz]}]
      [{:foo [(:bar {:b :c}) :baz]}]]
    '[{:foo [(:bar {:a :b}) :baz]}
      {:foo [(:bar {:b :c}) :baz]}]))


(deftest disambiguate-queries-with-and-without-conflicts
  (are [out in] (= out (om/disambiguate in))
    ;; Duplicate keywords are merged, conflicts are grouped
    ;; into two a sequence of two non-conflicting queries
    '[[:foo :bar :baz :ruux]
      [(:foo {:a :b})
       (:bar {:a :b})
       (:baz {:a :b})]]
    '[:foo (:foo {:a :b}) :foo
      :bar (:bar {:a :b}) :bar
      :baz (:baz {:a :b}) :baz
      :ruux]))

(ns workflo.macros.specs.query-test
  (:require [clojure.test :refer [are deftest is]]
            [clojure.spec :as s]
            [workflo.macros.query-new :as q]
            [workflo.macros.specs.query]))

(deftest conforming-regular-properties
  (are [out in] (= out (q/conform in))
    '[[:property [:simple a]]]
    '[a]

    '[[:property [:simple a]]
      [:property [:simple b]]]
    '[a b]

    '[[:property [:simple a]]
      [:property [:simple b]]
      [:property [:simple c]]]
    '[a b c]))

(deftest conforming-link-properties
  (are [out in] (= out (q/conform in))
    '[[:property [:link [a _]]]]
    '[[a _]]

    '[[:property [:link [a _]]]
      [:property [:link [b 1]]]]
    '[[a _] [b 1]]

    '[[:property [:link [a _]]]
      [:property [:link [b 1]]]
      [:property [:link [c :x]]]]
    '[[a _] [b 1] [c :x]]))

(deftest conforming-joins-with-a-simple-property-source
  (are [out in] (= out (q/conform in))
    '[[:property [:join [:properties {[:simple a]
                                      [[:property [:simple b]]]}]]]]
    '[{a [b]}]

    '[[:property [:join [:properties {[:simple a]
                                      [[:property [:simple b]]
                                       [:property [:simple c]]]}]]]]
    '[{a [b c]}]

    '[[:property [:join [:properties {[:simple a]
                                      [[:property [:simple b]]
                                       [:property [:simple c]]]}]]]
      [:property [:simple d]]]
    '[{a [b c]} d]

    '[[:property [:join [:properties {[:simple a]
                                      [[:property [:simple b]]
                                       [:property [:simple c]]]}]]]
      [:property [:join [:properties {[:simple d]
                                      [[:property [:simple e]]
                                       [:property [:simple f]]]}]]]]
    '[{a [b c]} {d [e f]}]

    '[[:property [:join [:recursive {[:simple a]
                                     [:unlimited ...]}]]]]
    '[{a ...}]

    '[[:property [:join [:recursive {[:simple a]
                                     [:limited 5]}]]]]
    '[{a 5}]

    '[[:property [:join [:model {[:simple a]
                                 [:model User]}]]]]
    '[{a User}]))

(deftest conforming-joins-with-a-link-source
  (are [out in] (= out (q/conform in))
    '[[:property [:join [:properties {[:link [a _]]
                                      [[:property [:simple b]]]}]]]]
    '[{[a _] [b]}]

    '[[:property [:join [:properties {[:link [a _]]
                                      [[:property [:simple b]]]}]]]]
    '[{[a _] [b]}]

    '[[:property [:join [:properties {[:link [a 1]]
                                      [[:property [:simple b]]
                                       [:property [:simple c]]]}]]]]
    '[{[a 1] [b c]}]

    '[[:property [:join [:properties {[:link [a :x]]
                                      [[:property [:simple b]]
                                       [:property [:simple c]]
                                       [:property [:simple d]]]}]]]]
    '[{[a :x] [b c d]}]))

(deftest conforming-prefixed-properties
  (are [out in] (= out (q/conform in))
    '[[:prefixed-properties {:base a
                             :children [[:property [:simple b]]]}]]
    '[a [b]]

    '[[:prefixed-properties {:base a
                             :children [[:property [:simple b]]
                                        [:property [:simple c]]]}]]
    '[a [b c]]

    '[[:prefixed-properties {:base a
                             :children [[:property [:simple b]]
                                        [:property [:simple c]]]}]
      [:property [:simple d]]]
    '[a [b c] d]

    '[[:prefixed-properties {:base a
                             :children [[:property [:simple b]]
                                        [:property [:simple c]]]}]
      [[:prefixed-properties {:base d
                              :children [[:property [:simple e]]
                                         [:property [:simple f]]]}]]]
    '[a [b c] d [e f]]))

(deftest conforming-aliased-regular-properties
  (are [out in] (= out (q/conform in))
    '[[:aliased-property {:property [:simple a] :as :as :alias b}]]
    '[a :as b]

    '[[:aliased-property {:property [:simple a] :as :as :alias b}]
      [:aliased-property {:property [:simple c] :as :as :alias d}]]
    '[a :as b c :as d]))

(deftest conforming-aliased-links
  (are [out in] (= out (q/conform in))
    '[[:aliased-property {:property [:link [a _]] :as :as :alias b}]]
    '[[a _] :as b]

    '[[:aliased-property {:property [:link [a 1]] :as :as :alias b}]]
    '[[a 1] :as b]

    '[[:aliased-property {:property [:link [a :x]] :as :as :alias b}]]
    '[[a :x] :as b]

    '[[:aliased-property {:property [:link [a _]] :as :as :alias b}]
      [:aliased-property {:property [:link [c _]] :as :as :alias d}]]
    '[[a _] :as b [c _] :as d]))

(deftest conforming-aliased-joins
  (are [out in] (= out (q/conform in))
    '[[:aliased-property
       {:property [:join [:properties {[:simple a]
                                       [[:property [:simple b]]]}]]
        :as :as :alias c}]]
    '[{a [b]} :as c]

    '[[:aliased-property
       {:property [:join [:properties {[:simple a]
                                       [[:property [:simple b]]
                                        [:property [:simple c]]]}]]
        :as :as :alias d}]
      [:aliased-property
       {:property [:join [:properties {[:simple e]
                                       [[:property [:simple f]]
                                        [:property [:simple g]]]}]]
        :as :as :alias h}]]
    '[{a [b c]} :as d {e [f g]} :as h]))

(deftest conforming-aliased-prefixed-properties
  (are [out in] (= out (q/conform in))
    '[[:prefixed-properties {:base a
                             :children [[:aliased-property
                                         {:property [:simple b]
                                          :as :as :alias c}]]}]]
    '[a [b :as c]]

    '[[:prefixed-properties {:base a
                             :children [[:aliased-property
                                         {:property [:simple b]
                                          :as :as :alias c}]
                                        [:aliased-property
                                         {:property [:simple d]
                                          :as :as :alias e}]]}]]
    '[a [b :as c d :as e]]))

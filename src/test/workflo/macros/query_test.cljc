(ns workflo.macros.query-test
  (:require [clojure.test :refer [are deftest is]]
            #?(:cljs [om.next])
            [workflo.macros.query :as q]
            [workflo.macros.query.om-next :as om]
            [workflo.macros.specs.query]))

;;;; Preparations for Om Next query generation in ClojureScript

#?(:cljs (om.next/defui User
           static om.next/IQuery
           (query [this]
             [:user/name :user/email])))

;;;; Conforming, parsing, Om Next query generation

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
    '[a b c]

    '[[:property [:simple a/b]]
      [:property [:simple b.c]]
      [:property [:simple b.c/d]]]
    '[a/b b.c b.c/d]))

(deftest parsing-regular-properties
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :property}]
    '[a]

    '[{:name a :type :property}
      {:name b :type :property}]
    '[a b]

    '[{:name a :type :property}
      {:name b :type :property}
      {:name c :type :property}]
    '[a b c]

    '[{:name a/b :type :property}
      {:name b.c :type :property}
      {:name b.c/d :type :property}]
    '[a/b b.c b.c/d]))

(deftest om-next-query-for-regular-properties
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[:a] '[a]
    '[:a :b] '[a b]
    '[:a :b :c] '[a b c]
    '[:a/b :b.c :b.c/d] '[a/b b.c b.c/d]))

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
    '[[a _] [b 1] [c :x]]

    '[[:property [:link [a/b _]]]
      [:property [:link [a.b/c _]]]]
    '[[a/b _] [a.b/c _]]))

(deftest parsing-link-properties
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :link :link-id _}]
    '[[a _]]

    '[{:name a :type :link :link-id _}
      {:name b :type :link :link-id 1}]
    '[[a _] [b 1]]

    '[{:name a :type :link :link-id _}
      {:name b :type :link :link-id 1}
      {:name c :type :link :link-id :x}]
    '[[a _] [b 1] [c :x]]

    '[{:name a/b :type :link :link-id _}
      {:name a.b/c :type :link :link-id _}]
    '[[a/b _] [a.b/c _]]))

(deftest om-next-query-for-link-properties
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[[:a _]] '[[a _]]
    '[[:a _] [:b 1]] '[[a _] [b 1]]
    '[[:a _] [:b 1] [:c :x]] '[[a _] [b 1] [c :x]]
    '[[:a/b _] [:a.b/c _]] '[[a/b _] [a.b/c _]]))

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

    '[[:property [:join [:properties {[:simple a/b]
                                      [[:property [:simple c]]]}]]]
      [:property [:join [:properties {[:simple a.b/c]
                                      [[:property [:simple d]]]}]]]]
    '[{a/b [c]} {a.b/c [d]}]

    '[[:property [:join [:recursive {[:simple a]
                                     [:unlimited ...]}]]]]
    '[{a ...}]

    '[[:property [:join [:recursive {[:simple a]
                                     [:limited 5]}]]]]
    '[{a 5}]

    '[[:property [:join [:view {[:simple a]
                                 [:view User]}]]]]
    '[{a User}]))

(deftest parsing-joins-with-a-simple-property-source
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}]}]
    '[{a [b]}]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]}]
    '[{a [b c]}]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]}
      {:name d :type :property}]
    '[{a [b c]} d]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]}
      {:name d :type :join
       :join-source {:name d :type :property}
       :join-target [{:name e :type :property}
                     {:name f :type :property}]}]
    '[{a [b c]} {d [e f]}]

    '[{:name a/b :type :join
       :join-source {:name a/b :type :property}
       :join-target [{:name c :type :property}]}
      {:name a.b/c :type :join
       :join-source {:name a.b/c :type :property}
       :join-target [{:name d :type :property}]}]
    '[{a/b [c]} {a.b/c [d]}]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target ...}]
    '[{a ...}]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target 5}]
    '[{a 5}]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target User}]
    '[{a User}]))

(deftest om-next-query-for-joins-with-a-simple-property-source
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{:a [:b]}] '[{a [b]}]
    '[{:a [:b :c]}] '[{a [b c]}]
    '[{:a [:b :c]} :d] '[{a [b c]} d]
    '[{:a [:b :c]} {:d [:e :f]}] '[{a [b c]} {d [e f]}]
    '[{:a/b [:c]} {:a.b/c [:d]}] '[{a/b [c]} {a.b/c [d]}]
    '[{:a ...}] '[{a ...}]
    '[{:a 5}] '[{a 5}]
    #?(:cljs '[{:a [:user/name :user/email]}]
       :clj  '[{:a (om.next/get-query
                    workflo.macros.query-test/User)}])
    '[{a workflo.macros.query-test/User}]))

(deftest conforming-joins-with-a-link-source
  (are [out in] (= out (q/conform in))
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
    '[{[a :x] [b c d]}]

    '[[:property [:join [:properties {[:link [a/b _]]
                                      [[:property [:simple c]]]}]]]]
    '[{[a/b _] [c]}]

    '[[:property [:join [:properties {[:link [a.b/c _]]
                                      [[:property [:simple d]]]}]]]]
    '[{[a.b/c _] [d]}]))

(deftest parsing-joins-with-a-link-source
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :join
       :join-source {:name a :type :link :link-id _}
       :join-target [{:name b :type :property}]}]
    '[{[a _] [b]}]

    '[{:name a :type :join
       :join-source {:name a :type :link :link-id 1}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]}]
    '[{[a 1] [b c]}]

    '[{:name a :type :join
       :join-source {:name a :type :link :link-id :x}
       :join-target [{:name b :type :property}
                     {:name c :type :property}
                     {:name d :type :property}]}]
    '[{[a :x] [b c d]}]

    '[{:name a/b :type :join
       :join-source {:name a/b :type :link :link-id _}
       :join-target [{:name c :type :property}]}]
    '[{[a/b _] [c]}]

    '[{:name a.b/c :type :join
       :join-source {:name a.b/c :type :link :link-id _}
       :join-target [{:name d :type :property}]}]
    '[{[a.b/c _] [d]}]))

(deftest om-next-query-for-joins-with-a-link-source
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{[:a _] [:b]}] '[{[a _] [b]}]
    '[{[:a 1] [:b :c]}] '[{[a 1] [b c]}]
    '[{[:a :x] [:b :c :d]}] '[{[a :x] [b c d]}]
    '[{[:a/b _] [:c]}] '[{[a/b _] [c]}]
    '[{[:a.b/c _] [:d]}] '[{[a.b/c _] [d]}]))

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
    '[a [b c] d [e f]]

    '[[:prefixed-properties {:base a.b
                             :children [[:property [:simple c]]]}]]
    '[a.b [c]]

    ;; JIRA issue CLJ-2003
    '[[:prefixed-properties {:base a
                             :children [[:property [:simple b]]
                                        [:property [:simple c]]]}]
      [[:prefixed-properties {:base d
                              :children [[:property [:simple e]]
                                         [:property [:simple f]]]}]
       [:prefixed-properties {:base h
                              :children [[:property [:simple i]]]}]]]
    '[a [b c] d [e f] h [i]]))

(deftest parsing-prefixed-properties
 (are [out in] (= out (q/conform-and-parse in))
   '[{:name a/b :type :property}]
   '[a [b]]

   '[{:name a/b :type :property}
     {:name a/c :type :property}]
   '[a [b c]]

   '[{:name a/b :type :property}
     {:name a/c :type :property}
     {:name d :type :property}]
   '[a [b c] d]

   '[{:name a/b :type :property}
     {:name a/c :type :property}
     {:name d/e :type :property}
     {:name d/f :type :property}]
   '[a [b c] d [e f]]

   '[{:name a.b/c :type :property}]
   '[a.b [c]]))

(deftest om-next-query-for-prefixed-properties
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[:a/b] '[a [b]]
    '[:a/b :a/c] '[a [b c]]
    '[:a/b :a/c :d/e :d/f] '[a [b c] d [e f]]
    '[:a.b/c] '[a.b [c]]))

;;;; Backref joins

(deftest conforming-backref-joins
  (are [out in] (= out (q/conform in))
    '[[:property
       [:join
        [:properties
         {[:simple a]
          [[:prefixed-properties
            {:base b
             :children [[:property
                         [:join
                          [:properties
                           {[:simple _c]
                            [[:property [:simple d]]]}]]]]}]]}]]]]
    '[{a [b [{_c [d]}]]}]))

(deftest parsing-backref-joins
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a
       :type :join
       :join-source {:name a :type :property}
       :join-target
       [{:name b/_c
         :type :join
         :join-source {:name b/_c :type :property}
         :join-target [{:name d :type :property}]}]}]
    '[{a [b [{_c [d]}]]}]))

(deftest om-next-query-for-backref-joins
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    [{:a [{:b/_c [:d]}]}]
    '[{a [b [{_c [d]}]]}]))

;;;; Aliases

(deftest conforming-aliased-regular-properties
  (are [out in] (= out (q/conform in))
    '[[:aliased-property {:property [:simple a] :as :as :alias b}]]
    '[a :as b]

    '[[:aliased-property {:property [:simple a] :as :as :alias b}]
      [:aliased-property {:property [:simple c] :as :as :alias d}]]
    '[a :as b c :as d]

    '[[:aliased-property {:property [:simple a/b] :as :as :alias c}]]
    '[a/b :as c]

    '[[:aliased-property {:property [:simple a.b/c] :as :as :alias d}]]
    '[a.b/c :as d]))

(deftest parsing-aliased-regular-properties
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :property :alias b}]
    '[a :as b]

    '[{:name a :type :property :alias b}
      {:name c :type :property :alias d}]
    '[a :as b c :as d]

    '[{:name a/b :type :property :alias c}]
    '[a/b :as c]

    '[{:name a.b/c :type :property :alias d}]
    '[a.b/c :as d]))

(deftest om-next-query-for-aliased-regular-properties
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[:a] '[a :as b]
    '[:a :c] '[a :as b c :as d]
    '[:a/b] '[a/b :as c]
    '[:a.b/c] '[a.b/c :as d]))

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
    '[[a _] :as b [c _] :as d]

    '[[:aliased-property {:property [:link [a/b _]] :as :as :alias c}]]
    '[[a/b _] :as c]

    '[[:aliased-property {:property [:link [a.b/c _]] :as :as :alias d}]]
    '[[a.b/c _] :as d]))

(deftest parsing-aliased-links
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :link :link-id _ :alias b}]
    '[[a _] :as b]

    '[{:name a :type :link :link-id 1 :alias b}]
    '[[a 1] :as b]

    '[{:name a :type :link :link-id :x :alias b}]
    '[[a :x] :as b]

    '[{:name a :type :link :link-id _ :alias b}
      {:name c :type :link :link-id _ :alias d}]
    '[[a _] :as b [c _] :as d]

    '[{:name a/b :type :link :link-id _ :alias c}]
    '[[a/b _] :as c]

    '[{:name a.b/c :type :link :link-id _ :alias d}]
    '[[a.b/c _] :as d]))

(deftest om-next-query-for-aliased-links
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[[:a _]] '[[a _] :as b]
    '[[:a 1]] '[[a 1] :as b]
    '[[:a :x]] '[[a :x] :as b]
    '[[:a _] [:c _]] '[[a _] :as b [c _] :as d]
    '[[:a/b _]] '[[a/b _] :as c]
    '[[:a.b/c _]] '[[a.b/c _] :as d]))

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
    '[{a [b c]} :as d {e [f g]} :as h]

    '[[:aliased-property
       {:property [:join [:properties {[:simple a/b]
                                       [[:property [:simple c]]]}]]
        :as :as :alias d}]]
    '[{a/b [c]} :as d]

    '[[:aliased-property
       {:property [:join [:properties {[:simple a.b/c]
                                       [[:property [:simple d]]]}]]
        :as :as :alias e}]]
    '[{a.b/c [d]} :as e]))

(deftest parsing-aliased-joins
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}]
       :alias c}]
    '[{a [b]} :as c]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]
       :alias d}
      {:name e :type :join
       :join-source {:name e :type :property}
       :join-target [{:name f :type :property}
                     {:name g :type :property}]
       :alias h}]
    '[{a [b c]} :as d {e [f g]} :as h]

    '[{:name a/b :type :join
       :join-source {:name a/b :type :property}
       :join-target [{:name c :type :property}]
       :alias d}]
    '[{a/b [c]} :as d]

    '[{:name a.b/c :type :join
       :join-source {:name a.b/c :type :property}
       :join-target [{:name d :type :property}]
       :alias e}]
    '[{a.b/c [d]} :as e]))

(deftest om-next-query-for-aliased-joins
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{:a [:b]}] '[{a [b]} :as c]
    '[{:a [:b :c]} {:e [:f :g]}] '[{a [b c]} :as d {e [f g]} :as h]
    '[{:a/b [:c]}] '[{a/b [c]} :as d]
    '[{:a.b/c [:d]}] '[{a.b/c [d]} :as e]))

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
    '[a [b :as c d :as e]]

    '[[:prefixed-properties {:base a.b
                             :children [[:aliased-property
                                         {:property [:simple c]
                                          :as :as :alias d}]]}]]
    '[a.b [c :as d]]))

(deftest parsing-aliased-prefixed-properties
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a/b :type :property :alias c}]
    '[a [b :as c]]

    '[{:name a/b :type :property :alias c}
      {:name a/d :type :property :alias e}]
    '[a [b :as c d :as e]]

    '[{:name a.b/c :type :property :alias d}]
    '[a.b [c :as d]]))

(deftest om-next-query-for-aliased-prefixed-properties
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[:a/b] '[a [b :as c]]
    '[:a/b :a/d] '[a [b :as c d :as e]]
    '[:a.b/c] '[a.b [c :as d]]))

(q/register-query-fragment! :properties-fragment '[foo bar])
(q/register-query-fragment! :join-fragment '[{baz [ruux]}])
(q/register-query-fragment! :prefixed-fragment '[base [child]])

(deftest conforming-query-fragments
  (are [out in] (= out (q/conform in))
    '[[:fragment ...properties-fragment]]
    '[...properties-fragment]

    '[[:fragment ...join-fragment]]
    '[...join-fragment]

    '[[:fragment ...prefixed-fragment]]
    '[...prefixed-fragment]))

(deftest parsing-query-fragments
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name foo :type :property}
      {:name bar :type :property}]
    '[...properties-fragment]

    '[{:name baz
       :type :join
       :join-source {:name baz :type :property}
       :join-target [{:name ruux :type :property}]}]
    '[...join-fragment]

    '[{:name base/child :type :property}]
    '[...prefixed-fragment]))

(deftest parsing-non-existent-query-fragments
  (is (thrown? #?(:cljs js/Error :clj Exception)
               (q/conform-and-parse '[...non-existent-fragment]))))

(deftest om-next-query-for-query-fragments
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[:foo :bar] '[...properties-fragment]
    '[{:baz [:ruux]}] '[...join-fragment]
    '[:base/child] '[...prefixed-fragment]))

(deftest conforming-parameterization
  (are [out in] (= out (q/conform in))
    '[[:parameterization {:query [:property [:simple a]]
                          :parameters {b c}}]]
    '[(a {b c})]

    '[[:parameterization {:query [:property [:simple a]]
                          :parameters {b c d e}}]]
    '[(a {b c d e})]

    '[[:parameterization
       {:query
        [:property [:join [:properties {[:simple a]
                                        [[:property [:simple b]]
                                         [:property [:simple c]]]}]]]
        :parameters {d e f g}}]]
    '[({a [b c]} {d e f g})]

    '[[:parameterization
       {:query [:aliased-property {:property [:simple a]
                                   :as :as :alias b}]
        :parameters {c d e f}}]]
    '[(a :as b {c d e f})]

    '[[:parameterization
       {:query
        [:aliased-property
         {:property [:join [:properties {[:simple a]
                                         [[:property [:simple b]]
                                          [:property [:simple c]]]}]]
          :as :as :alias d}]
        :parameters {e f g h}}]]
    '[({a [b c]} :as d {e f g h})]

    '[[:parameterization
       {:query [:aliased-property {:property [:simple a/b]
                                   :as :as :alias c}]
        :parameters {d e f g}}]]
    '[(a/b :as c {d e f g})]

    '[[:parameterization {:query [:property [:simple a]]
                          :parameters {[b c d] e}}]]
    '[(a {[b c d] e})]))

(deftest parsing-parameterization
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name a :type :property :parameters {b c}}]
    '[(a {b c})]

    '[{:name a :type :property :parameters {b c d e}}]
    '[(a {b c d e})]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]
       :parameters {d e f g}}]
    '[({a [b c]} {d e f g})]

    '[{:name a :type :property :alias b :parameters {c d e f}}]
    '[(a :as b {c d e f})]

    '[{:name a :type :join
       :join-source {:name a :type :property}
       :join-target [{:name b :type :property}
                     {:name c :type :property}]
       :alias d
       :parameters {e f g h}}]
    '[({a [b c]} :as d {e f g h})]

    '[{:name a/b :type :property :alias c :parameters {d e f g}}]
    '[(a/b :as c {d e f g})]

    '[{:name a :type :property :parameters {[b c d] e}}]
    '[(a {[b c d] e})]))

(deftest om-next-query-for-parameterizations
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    #?(:cljs '[(:a {:b 'c})]
       :clj  '[(:a {:b 'c})])
    '[(a {b c})]

    #?(:cljs '[(:a {:b 'c :d 'e})]
       :clj  '[(:a {:b 'c :d 'e})])
    '[(a {b c d e})]

    #?(:cljs '[({:a [:b :c]} {:d 'e :f 'g})]
       :clj  '[({:a [:b :c]} {:d 'e :f 'g})])
    '[({a [b c]} {d e f g})]

    #?(:cljs '[(:a {:c 'd :e 'f})]
       :clj  '[(:a {:c 'd :e 'f})])
    '[(a :as b {c d e f})]

    #?(:cljs '[({:a [:b :c]} {:e 'f :g 'h})]
       :clj  '[({:a [:b :c]} {:e 'f :g 'h})])
    '[({a [b c]} :as d {e f g h})]

    '[(:a/b {:d 'e :f 'g})]
    '[(a/b :as c {d e f g})]

    #?(:cljs '[(:a {[:b :c :d] 'c})]
       :clj  '[(:a {[:b :c :d] 'c})])
    '[(a {[b c d] c})]))

(deftest conforming-joins-with-sub-joins
  (are [out in] (= out (q/conform in))
    '[[:property
       [:join
        [:properties
         {[:simple users]
          [[:prefixed-properties
            {:base db
             :children [[:property [:simple id]]]}]
           [:prefixed-properties
            {:base user
             :children [[:property [:simple name]]]}]
           [:property
            [:join
             [:properties
              {[:simple friends]
               [[:prefixed-properties
                 {:base db
                  :children [[:property [:simple id]]]}]
                [:prefixed-properties
                 {:base user
                  :children [[:property [:simple name]]]}]]}]]]]}]]]]
    '[{users [db [id]
              user [name]
              {friends [db [id]
                        user [name]]}]}]

    '[[:property
       [:join
        [:properties
         {[:simple users]
          [[:property
            [:join
             [:properties
              {[:simple friends]
               [[:property
                 [:join
                  [:properties
                   {[:simple friends]
                    [[:prefixed-properties
                      {:base db
                       :children
                       [[:property [:simple id]]]}]]}]]]]}]]]]}]]]]
    '[{users [{friends [{friends [db [id]]}]}]}]))

(deftest parsing-joins-with-sub-joins
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name users :type :join
       :join-source {:name users :type :property}
       :join-target
       [{:name db/id :type :property}
        {:name user/name :type :property}
        {:name friends :type :join
         :join-source {:name friends :type :property}
         :join-target [{:name db/id :type :property}
                       {:name user/name :type :property}]}]}]
    '[{users [db [id]
              user [name]
              {friends [db [id]
                        user [name]]}]}]

    '[{:name users :type :join
       :join-source {:name users :type :property}
       :join-target
       [{:name friends :type :join
         :join-source {:name friends :type :property}
         :join-target
         [{:name friends :type :join
           :join-source {:name friends :type :property}
           :join-target [{:name db/id :type :property}]}]}]}]
    '[{users [{friends [{friends [db [id]]}]}]}]))

(deftest om-next-query-for-joins-with-sub-joins
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{:users [:db/id :user/name {:friends [:db/id :user/name]}]}]
    '[{users [db [id]
              user [name]
              {friends [db [id]
                        user [name]]}]}]

    '[{:users [{:friends [{:friends [:db/id]}]}]}]
    '[{users [{friends [{friends [db [id]]}]}]}]))

(deftest conforming-joins-with-sub-links
  (are [out in] (= out (q/conform in))
    '[[:property
       [:join
        [:properties
         {[:simple users]
          [[:prefixed-properties
            {:base db
             :children [[:property [:simple id]]]}]
           [:property [:link [current-user _]]]]}]]]]
    '[{users [db [id] [current-user _]]}]

    '[[:property
       [:join
        [:properties
         {[:simple users]
          [[:prefixed-properties
            {:base user
             :children [[:property [:simple name]]]}]
           [:property
            [:join
             [:properties
              {[:link [current-user _]]
               [[:prefixed-properties
                 {:base user
                  :children [[:property [:simple name]]]}]]}]]]]}]]]]
    '[{users [user [name] {[current-user _] [user [name]]}]}]))

(deftest parsing-joins-with-sub-links
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name users :type :join
       :join-source {:name users :type :property}
       :join-target [{:name db/id :type :property}
                     {:name current-user :type :link :link-id _}]}]
    '[{users [db [id] [current-user _]]}]

    '[{:name users :type :join
       :join-source {:name users :type :property}
       :join-target
       [{:name user/name :type :property}
        {:name current-user :type :join
         :join-source {:name current-user :type :link :link-id _}
         :join-target [{:name user/name :type :property}]}]}]
    '[{users [user [name] {[current-user _] [user [name]]}]}]))

(deftest om-next-query-for-joins-with-sub-links
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{:users [:db/id [:current-user _]]}]
    '[{users [db [id] [current-user _]]}]

    '[{:users [:user/name {[:current-user _] [:user/name]}]}]
    '[{users [user [name] {[current-user _] [user [name]]}]}]))

(deftest conforming-joins-with-sub-aliases
  (are [out in] (= out (q/conform in))
    '[[:property
       [:join
        [:properties
         {[:link [user 1]]
          [[:prefixed-properties
            {:base db
             :children [[:aliased-property
                         {:property [:simple id]
                          :as :as :alias db-id}]]}]
           [:aliased-property {:property [:simple name]
                               :as :as :alias nm}]]}]]]]
    '[{[user 1] [db [id :as db-id] name :as nm]}]))

(deftest parsing-joins-with-sub-aliases
  (are [out in] (= out (q/conform-and-parse in))
    '[{:name user :type :join
       :join-source {:name user :type :link :link-id 1}
       :join-target [{:name db/id :type :property :alias db-id}
                     {:name name :type :property :alias nm}]}]
    '[{[user 1] [db [id :as db-id] name :as nm]}]))

(deftest om-next-query-for-joins-with-sub-aliases
  (are [out in] (= out (-> in q/conform-and-parse om/query))
    '[{[:user 1] [:db/id :name]}]
    '[{[user 1] [db [id :as db-id] name :as nm]}]))

;;;; Map destructuring

(deftest map-destructuring-keys
  (and (is (= (-> '[foo bar baz]
                  q/conform-and-parse q/map-destructuring-keys)
              '[foo bar baz]))
       (is (= (-> '[foo [bar baz]]
                  q/conform-and-parse q/map-destructuring-keys)
              '[foo/bar foo/baz]))
       (is (= (-> '[{foo Foo} {bar Bar}]
                  q/conform-and-parse q/map-destructuring-keys)
              '[foo bar]))
       (is (= (-> '[[foo _] [bar 123] [baz :baz]]
                  q/conform-and-parse q/map-destructuring-keys)
              '[foo bar baz]))
       (is (= (-> '[foo [{bar Bar} [baz 123]]]
                  q/conform-and-parse q/map-destructuring-keys)
              '[foo/bar foo/baz]))
       (is (= (-> '[a/b a.b/c]
                  q/conform-and-parse q/map-destructuring-keys)
              '[a/b a.b/c]))))

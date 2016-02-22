(ns app-macros.props-test
  (:require [cljs.test :refer-macros [deftest is]]
            [app-macros.props :as p]))

;;;; Padding

(deftest pad-by-equality
  (is (= (into [] (p/pad-by = nil [1 1 2 3 4 4 5]))
         [1 nil 1 2 3 4 nil 4 5])))

;;;; Value testing

(deftest values
  (and (is (p/value? 5))
       (is (p/value? 10))
       (is (p/value? "Hello"))
       (is (p/value? "World"))
       (is (p/value? :foo))
       (is (p/value? :bar))
       (is (p/value? '_))))

;;;; Property types

(deftest property-types
  (and (is (= :property (p/property-type 'foo)))
       (is (= :property (p/property-type 'bar)))
       (is (= :link     (p/property-type '[foo 5])))
       (is (= :link     (p/property-type '[bar "Hello"])))
       (is (= :link     (p/property-type '[baz :hello])))
       (is (= :join     (p/property-type '{foo ...})))
       (is (= :join     (p/property-type '{bar Hello})))))

;;;; Parsing

(deftest parse-one-prop
  (and (is (= (p/parse '[foo]) '[{:name foo :type :property}]))
       (is (= (p/parse '[bar]) '[{:name bar :type :property}]))))

(deftest parse-two-props
  (is (= (p/parse '[foo bar])
         '[{:name foo :type :property}
           {:name bar :type :property}])))

(deftest parse-one-ident
  (and (is (= (p/parse '[[user 5]])
              '[{:name user :type :link :target 5}]))
       (is (= (p/parse '[[user "foo"]])
              '[{:name user :type :link :target "foo"}]))
       (is (= (p/parse '[[user :foo]])
              '[{:name user :type :link :target :foo}]))))

(deftest parse-two-idents
  (and (is (= (p/parse '[[user 5] [post 10]])
              '[{:name user :type :link :target 5}
                {:name post :type :link :target 10}]))))

(deftest parse-one-join
  (and (is (= (p/parse '[{foo ...}])
              '[{:name foo :type :join :target ...}]))
       (is (= (p/parse '[{bar User}])
              '[{:name bar :type :join :target User}]))
       (is (= (p/parse '[{baz 5}])
              '[{:name baz :type :join :target 5}]))))

(deftest parse-three-joins
  (and (is (= (p/parse '[{foo ...}
                         {bar User}
                         {baz 5}])
              '[{:name foo :type :join :target ...}
                {:name bar :type :join :target User}
                {:name baz :type :join :target 5}]))))

(deftest parse-basic-child-props
  (and (is (= (p/parse '[foo [bar]])
              '[{:name foo/bar :type :property}]))
       (is (= (p/parse '[foo [bar baz]])
              '[{:name foo/bar :type :property}
                {:name foo/baz :type :property}]))
       (is (= (p/parse '[foo [bar] baz [ruux]])
              '[{:name foo/bar :type :property}
                {:name baz/ruux :type :property}]))
       (is (= (p/parse '[foo bar [baz] ruux])
              '[{:name foo :type :property}
                {:name bar/baz :type :property}
                {:name ruux :type :property}]))))

(deftest parse-join-child-props
  (and (is (= (p/parse '[foo [{bar Baz}]])
              '[{:name foo/bar :type :join :target Baz}]))
       (is    (= (p/parse '[foo [{bar Baz} {baz Ruux}]])
                 '[{:name foo/bar :type :join :target Baz}
                   {:name foo/baz :type :join :target Ruux}]))
       (is (= (p/parse '[foo [{bar ...}]])
              '[{:name foo/bar :type :join :target ...}]))
       (is (= (p/parse '[foo [{bar 5}]])
              '[{:name foo/bar :type :join :target 5}]))))

(deftest parse-link-child-props
  (and (is (= (p/parse '[foo [[bar _]]])
              '[{:name foo/bar :type :link :target _}]))
       (is (= (p/parse '[foo [[bar 15]]])
              '[{:name foo/bar :type :link :target 15}]))
       (is (= (p/parse '[foo [[bar :baz]]])
              '[{:name foo/bar :type :link :target :baz}]))
       (is (= (p/parse '[foo [[bar _] [baz 5]]])
              '[{:name foo/bar :type :link :target _}
                {:name foo/baz :type :link :target 5}]))
       (is (= (p/parse '[foo [[bar _]] baz [[ruux 15]]])
              '[{:name foo/bar :type :link :target _}
                {:name baz/ruux :type :link :target 15}]))))

;;;; Om Next query generation

(deftest basic-queries
  (and (is (= (-> '[foo bar baz] p/parse p/om-query)
              [:foo :bar :baz]))
       (is (= (-> '[foo [bar baz] ruux] p/parse p/om-query)
              [:foo/bar :foo/baz :ruux]))))

(deftest queries-with-joins
  (and (is (= (-> '[{foo User}] p/parse p/om-query)
              '[{:foo (om.next/get-query User)}]))
       (is (= (-> '[{foo ...}] p/parse p/om-query)
              '[{:foo ...}]))
       (is (= (-> '[{foo 17}] p/parse p/om-query)
              '[{:foo 17}]))
       (is (= (-> '[foo [{bar User}]] p/parse p/om-query)
              '[{:foo/bar (om.next/get-query User)}]))))

(deftest queries-with-links
  (and (is (= (-> '[[current-user _]] p/parse p/om-query)
              '[[:current-user _]]))
       (is (= (-> '[[user 123]] p/parse p/om-query)
              '[[:user 123]]))
       (is (= (-> '[[user "Jeff"]] p/parse p/om-query)
              '[[:user "Jeff"]]))
       (is (= (-> '[[user :jeff]] p/parse p/om-query)
              '[[:user :jeff]]))
       (is (= (-> '[user [name [friend 123]]] p/parse p/om-query)
              '[:user/name [:user/friend 123]]))))

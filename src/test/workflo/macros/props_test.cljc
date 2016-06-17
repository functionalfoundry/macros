(ns workflo.macros.props-test
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [cljs.spec.test :refer [check-var]]
               :clj  [clojure.spec.test :refer [check-var]])
            [clojure.test.check.generators :as gen]
            [workflo.macros.props :as p]))

;;;; Run random tests for prop spec parsing functions

(clojure.spec.test/run-tests 'workflo.macros.props)

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
              '[{:name user :type :link :link-id 5}]))
       (is (= (p/parse '[[user "foo"]])
              '[{:name user :type :link :link-id "foo"}]))
       (is (= (p/parse '[[user :foo]])
              '[{:name user :type :link :link-id :foo}]))))

(deftest parse-two-idents
  (and (is (= (p/parse '[[user 5] [post 10]])
              '[{:name user :type :link :link-id 5}
                {:name post :type :link :link-id 10}]))))

(deftest parse-one-join
  (and (is (= (p/parse '[{foo ...}])
              '[{:name foo :type :join :join-target ...}]))
       (is (= (p/parse '[{bar User}])
              '[{:name bar :type :join :join-target User}]))
       (is (= (p/parse '[{baz 5}])
              '[{:name baz :type :join :join-target 5}]))))

(deftest parse-three-joins
  (and (is (= (p/parse '[{foo ...}
                         {bar User}
                         {baz 5}])
              '[{:name foo :type :join :join-target ...}
                {:name bar :type :join :join-target User}
                {:name baz :type :join :join-target 5}]))))

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
              '[{:name foo/bar :type :join :join-target Baz}]))
       (is    (= (p/parse '[foo [{bar Baz} {baz Ruux}]])
                 '[{:name foo/bar :type :join :join-target Baz}
                   {:name foo/baz :type :join :join-target Ruux}]))
       (is (= (p/parse '[foo [{bar ...}]])
              '[{:name foo/bar :type :join :join-target ...}]))
       (is (= (p/parse '[foo [{bar 5}]])
              '[{:name foo/bar :type :join :join-target 5}]))))

(deftest parse-link-child-props
  (and (is (= (p/parse '[foo [[bar _]]])
              '[{:name foo/bar :type :link :link-id _}]))
       (is (= (p/parse '[foo [[bar 15]]])
              '[{:name foo/bar :type :link :link-id 15}]))
       (is (= (p/parse '[foo [[bar :baz]]])
              '[{:name foo/bar :type :link :link-id :baz}]))
       (is (= (p/parse '[foo [[bar _] [baz 5]]])
              '[{:name foo/bar :type :link :link-id _}
                {:name foo/baz :type :link :link-id 5}]))
       (is (= (p/parse '[foo [[bar _]] baz [[ruux 15]]])
              '[{:name foo/bar :type :link :link-id _}
                {:name baz/ruux :type :link :link-id 15}]))))

;;;;;; Om Next query generation
;;
;;(deftest basic-queries
;;  (and (is (= (-> '[foo bar baz] p/parse p/om-query)
;;              [:foo :bar :baz]))
;;       (is (= (-> '[foo [bar baz] ruux] p/parse p/om-query)
;;              [:foo/bar :foo/baz :ruux]))))
;;
;;(deftest queries-with-joins
;;  (and (is (= (-> '[{foo User}] p/parse p/om-query)
;;              '[{:foo (om.next/get-query User)}]))
;;       (is (= (-> '[{foo ...}] p/parse p/om-query)
;;              '[{:foo '...}]))
;;       (is (= (-> '[{foo 17}] p/parse p/om-query)
;;              '[{:foo 17}]))
;;       (is (= (-> '[foo [{bar User}]] p/parse p/om-query)
;;              '[{:foo/bar (om.next/get-query User)}]))))
;;
;;(deftest queries-with-links
;;  (and (is (= (-> '[[current-user _]] p/parse p/om-query)
;;              '[[:current-user '_]]))
;;       (is (= (-> '[[user 123]] p/parse p/om-query)
;;              '[[:user 123]]))
;;       (is (= (-> '[[user "Jeff"]] p/parse p/om-query)
;;              '[[:user "Jeff"]]))
;;       (is (= (-> '[[user :jeff]] p/parse p/om-query)
;;              '[[:user :jeff]]))
;;       (is (= (-> '[user [name [friend 123]]] p/parse p/om-query)
;;              '[:user/name [:user/friend 123]]))))
;;
;;;;;; Map destructuring
;;
;;(deftest map-keys
;;  (and (is (= (-> '[foo bar baz] p/parse p/map-keys)
;;              '[foo bar baz]))
;;       (is (= (-> '[foo [bar baz]] p/parse p/map-keys)
;;              '[foo/bar foo/baz]))
;;       (is (= (-> '[{foo Foo} {bar Bar}] p/parse p/map-keys)
;;              '[foo bar]))
;;       (is (= (-> '[[foo _] [bar 123] [baz :baz]] p/parse p/map-keys)
;;              '[foo bar baz]))
;;       (is (= (-> '[foo [{bar Bar} [baz 123]]] p/parse p/map-keys)
;;              '[foo/bar foo/baz]))))

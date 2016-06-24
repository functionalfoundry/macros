(ns workflo.macros.query-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            [workflo.macros.query :as q]))

;;;; Parsing

(deftest parse-one-prop
  (and (is (= (q/parse '[foo]) '[{:name foo :type :property}]))
       (is (= (q/parse '[bar]) '[{:name bar :type :property}]))))

(deftest parse-two-props
  (is (= (q/parse '[foo bar])
         '[{:name foo :type :property}
           {:name bar :type :property}])))

(deftest parse-one-ident
  (and (is (= (q/parse '[[user 5]])
              '[{:name user :type :link :link-id 5}]))
       (is (= (q/parse '[[user "foo"]])
              '[{:name user :type :link :link-id "foo"}]))
       (is (= (q/parse '[[user :foo]])
              '[{:name user :type :link :link-id :foo}]))))

(deftest parse-two-idents
  (and (is (= (q/parse '[[user 5] [post 10]])
              '[{:name user :type :link :link-id 5}
                {:name post :type :link :link-id 10}]))))

(deftest parse-one-join
  (and (is (= (q/parse '[{foo ...}])
              '[{:name foo :type :join :join-target ...}]))
       (is (= (q/parse '[{bar User}])
              '[{:name bar :type :join :join-target User}]))
       (is (= (q/parse '[{baz 5}])
              '[{:name baz :type :join :join-target 5}]))))

(deftest parse-three-joins
  (and (is (= (q/parse '[{foo ...}
                         {bar User}
                         {baz 5}])
              '[{:name foo :type :join :join-target ...}
                {:name bar :type :join :join-target User}
                {:name baz :type :join :join-target 5}]))))

(deftest parse-basic-child-props
  (and (is (= (q/parse '[foo [bar]])
              '[{:name foo/bar :type :property}]))
       (is (= (q/parse '[foo [bar baz]])
              '[{:name foo/bar :type :property}
                {:name foo/baz :type :property}]))
       (is (= (q/parse '[foo [bar] baz [ruux]])
              '[{:name foo/bar :type :property}
                {:name baz/ruux :type :property}]))
       (is (= (q/parse '[foo bar [baz] ruux])
              '[{:name foo :type :property}
                {:name bar/baz :type :property}
                {:name ruux :type :property}]))))

(deftest parse-join-child-props
  (and (is (= (q/parse '[foo [{bar Baz}]])
              '[{:name foo/bar :type :join :join-target Baz}]))
       (is    (= (q/parse '[foo [{bar Baz} {baz Ruux}]])
                 '[{:name foo/bar :type :join :join-target Baz}
                   {:name foo/baz :type :join :join-target Ruux}]))
       (is (= (q/parse '[foo [{bar ...}]])
              '[{:name foo/bar :type :join :join-target ...}]))
       (is (= (q/parse '[foo [{bar 5}]])
              '[{:name foo/bar :type :join :join-target 5}]))))

(deftest parse-link-child-props
  (and (is (= (q/parse '[foo [[bar _]]])
              '[{:name foo/bar :type :link :link-id _}]))
       (is (= (q/parse '[foo [[bar 15]]])
              '[{:name foo/bar :type :link :link-id 15}]))
       (is (= (q/parse '[foo [[bar :baz]]])
              '[{:name foo/bar :type :link :link-id :baz}]))
       (is (= (q/parse '[foo [[bar _] [baz 5]]])
              '[{:name foo/bar :type :link :link-id _}
                {:name foo/baz :type :link :link-id 5}]))
       (is (= (q/parse '[foo [[bar _]] baz [[ruux 15]]])
              '[{:name foo/bar :type :link :link-id _}
                {:name baz/ruux :type :link :link-id 15}]))))

;;;;;; Om Next query generation

(deftest basic-queries
  (and (is (= (-> '[foo bar baz] q/parse q/om-query)
              [:foo :bar :baz]))
       (is (= (-> '[foo [bar baz] ruux] q/parse q/om-query)
              [:foo/bar :foo/baz :ruux]))))

(deftest queries-with-joins
  (and (is (= (-> '[{foo User}] q/parse q/om-query)
              '[{:foo (om.next/get-query User)}]))
       (is (= (-> '[{foo ...}] q/parse q/om-query)
              '[{:foo '...}]))
       (is (= (-> '[{foo 17}] q/parse q/om-query)
              '[{:foo 17}]))
       (is (= (-> '[foo [{bar User}]] q/parse q/om-query)
              '[{:foo/bar (om.next/get-query User)}]))))

(deftest queries-with-links
  (and (is (= (-> '[[current-user _]] q/parse q/om-query)
              '[[:current-user '_]]))
       (is (= (-> '[[user 123]] q/parse q/om-query)
              '[[:user 123]]))
       (is (= (-> '[[user "Jeff"]] q/parse q/om-query)
              '[[:user "Jeff"]]))
       (is (= (-> '[[user :jeff]] q/parse q/om-query)
              '[[:user :jeff]]))
       (is (= (-> '[user [name [friend 123]]] q/parse q/om-query)
              '[:user/name [:user/friend 123]]))))

;;;; Map destructuring

(deftest map-destructuring-keys
  (and (is (= (-> '[foo bar baz]
                  q/parse q/map-destructuring-keys)
              '[foo bar baz]))
       (is (= (-> '[foo [bar baz]]
                  q/parse q/map-destructuring-keys)
              '[foo/bar foo/baz]))
       (is (= (-> '[{foo Foo} {bar Bar}]
                  q/parse q/map-destructuring-keys)
              '[foo bar]))
       (is (= (-> '[[foo _] [bar 123] [baz :baz]]
                  q/parse q/map-destructuring-keys)
              '[foo bar baz]))
       (is (= (-> '[foo [{bar Bar} [baz 123]]]
                  q/parse q/map-destructuring-keys)
              '[foo/bar foo/baz]))))

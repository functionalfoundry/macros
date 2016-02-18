(ns app-macros.view-test
  (:require [cljs.test :refer-macros [deftest is]]
            [om.next :as om]
            [app-macros.view :refer-macros [defview]]))

(defview MinimalView)

(deftest minimal-view-works
  (and (is (not (nil? MinimalView)))
       (is (not (nil? minimal-view)))))

(defview MinimalViewWithKeyfn
  (keyfn (str (:foo props) "-baz")))

(deftest minimal-view-with-key-fn-works
  (let [view (minimal-view-with-keyfn {:foo "bar"})]
    (is (= (.-key view) "bar-baz"))))

(defview ViewWithQuery
  (query [:foo :bar]))

(deftest view-with-query-works
  (is (= [:foo :bar] (om/get-query ViewWithQuery))))

(defview ViewWithProps
  [user [name email]]
  (ident [:user/by-name name])
  (get-name name)
  (get-email email))

(comment
 (deftest view-with-props-works
   (let [view (view-with-props {:user/name "Jeff"
                                :user/email "jeff@jeff.org"})]
     (println (.-prototype (type view)))
     (println (.-om$isComponent view))
     (and (is (= "Jeff" (.get-name view)))
          (is (= "jeff@jeff.org" (.get-email view))))))

 (deftest foo
   (cljs.pprint/pprint
    (macroexpand-1
     '(defview ViewWithProps
        [user [name email]]
        (ident [:user/by-name name])
        (keyfn name)
        (validator foo)
        (query-params {:foo :bar})
        (query [:foo :bar])
        (get-name name foo bar)
        (get-email email))))))

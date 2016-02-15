(ns app-macros.view-test
  (:require [cljs.test :refer-macros [deftest is]]
            [om.next :as om]
            [app-macros.view :refer-macros [defview]]))

(defview MinimalView)

(deftest minimal-view-works
  (and (is (not (nil? MinimalView)))
       (is (not (nil? minimal-view)))))

(defview MinimalViewWithKeyfn
  (keyfn [{:keys [foo]}] (str foo "-baz")))

(deftest minimal-view-with-key-fn-works
  (and (is (not (nil? MinimalViewWithKeyfn)))
       (is (not (nil? minimal-view-with-keyfn)))
       (is (not (nil? minimal-view-with-keyfn-keyfn)))
       (is (= "bar-baz" (minimal-view-with-keyfn-keyfn {:foo "bar"})))))

(defview ViewWithQuery
  (query [this]
    [:foo :bar]))

(deftest view-with-query-works
  (is (= [:foo :bar] (om/get-query ViewWithQuery))))

(defview ViewWithProps
  [user [name email]]
  (ident [this props] [:user/by-name name])
  (get-name [this] name)
  (get-email [this] email))

(comment
  (deftest view-with-props-works
    (let [view (view-with-props {:user/name "Jeff"
                                 :user/email "jeff@jeff.org"})]
      (println (.-prototype (type view)))
      (println (.-om$isComponent view))
      (and (is (= "Jeff" (.get-name view)))
           (is (= "jeff@jeff.org" (.get-email view))))))

  (cljs.pprint/pprint
   (macroexpand-1
    '(defview ViewWithProps
       [user [name email]]
       (ident [this props] [:user/by-name name])
       (get-name [this] name)
       (get-email [this] email)))))

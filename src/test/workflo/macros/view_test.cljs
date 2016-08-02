(ns workflo.macros.view-test
  (:require [cljs.test :refer-macros [deftest is]]
            [workflo.macros.view :as view :refer-macros [defview]]))

(deftest minimal-view-definition
  (try
    (is (= (macroexpand-1
            '(defview MinimalView))
           '(do
              (om.next/defui MinimalView)
              (def minimal-view
                (workflo.macros.view/factory MinimalView {}))
              (workflo.macros.view/register-view!
               'MinimalView {:view MinimalView
                             :factory minimal-view}))))
    (catch js/Object e
      (println e))))

(deftest view-definition-with-props
  (is (= (macroexpand-1
          '(defview View
             (query [user [name email]])
             (key name)))
         '(do
            (om.next/defui View
              static om.next/IQuery
              (query [this]
                [:user/name :user/email]))
            (def view
              (workflo.macros.view/factory View
                {:keyfn (fn [props]
                          (let [{:keys [user/name
                                        user/email]} props]
                            name))}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-computed-props
  (is (= (macroexpand-1
          '(defview View
             (query [user [name email]])
             (computed [on-click])
             (key name)))
         '(do
            (om.next/defui View
              static om.next/IQuery
              (query [this]
                [:user/name :user/email]))
            (def view
              (workflo.macros.view/factory View
                {:keyfn
                 (fn [props]
                   (let [{:keys [user/name user/email]} props
                         {:keys [on-click]} (om/get-computed props)]
                     name))}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-implicit-ident-and-keyfn-via-db-id
  (is (= (macroexpand-1
          '(defview View
             (query [db [id]])))
         '(do
            (om.next/defui View
              static om.next/Ident
              (ident [this props]
                (let [{:keys [db/id]} props]
                  [:db/id id]))
              static om.next/IQuery
              (query [this]
                [:db/id]))
            (def view
              (workflo.macros.view/factory View
                {:keyfn (fn [props]
                          (let [{:keys [db/id]} props]
                            id))}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-simple-keyfn-is-correct
  (is (= (macroexpand-1
          '(defview View
             (key :foo)))
         '(do
            (om.next/defui View)
            (def view
              (workflo.macros.view/factory View
                {:keyfn (fn [props] :foo)}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-overriden-query-and-keyfn
  (is (= (macroexpand-1
          '(defview View
             (query [:custom :query])
             (key :custom)))
         '(do
            (om.next/defui View
              static om.next/IQuery
              (query [this]
                [:custom :query]))
            (def view
              (workflo.macros.view/factory View
                {:keyfn (fn [props] :custom)}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-overriden-ident
  (is (= (macroexpand-1
          '(defview View
             (query [db [id] user [name]])
             (ident [:user/by-name name])))
         '(do
            (om.next/defui View
              static om.next/Ident
              (ident [this props]
                (let [{:keys [db/id user/name]} props]
                  [:user/by-name name]))
              static om.next/IQuery
              (query [this]
                [:db/id :user/name]))
            (def view
              (workflo.macros.view/factory View
                {:keyfn (fn [props]
                          (let [{:keys [db/id user/name]} props]
                            id))}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-definition-with-raw-function
  (is (= (macroexpand-1
          '(defview View
             (query [user [name]])
             (.on-click [this]
              (js/alert name))))
         '(do
            (om.next/defui View
              Object
              (on-click [this]
                (let [{:keys [user/name]} (om/props this)]
                  (js/alert name)))
              static om.next/IQuery
              (query [this]
                [:user/name]))
            (def view
              (workflo.macros.view/factory View {}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(defview Wrapper
  (render
    (om.dom/div nil
      (om.next/children this))))

(deftest view-definition-with-wrapper-and-multiple-render-children
  (view/configure-views! {:wrapper-view wrapper})
  (is (= (macroexpand-1
          '(defview View
             (render
               (foo)
               (bar))))
         '(do
            (om.next/defui View
              Object
              (render [this]
                ((workflo.macros.view/wrapper)
                  (om.next/props this)
                  (foo)
                  (bar))))
            (def view
              (workflo.macros.view/factory View {}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

(deftest view-with-commands
  (is (= (macroexpand-1
          '(defview View
             (commands [goto show-foo])
             (render
              (foo {:on-click #(goto 'some-screen {:id 1})}))))
         '(do
            (om.next/defui View
              Object
              (render [this]
                (let [goto     (fn [params & reads]
                                 (workflo.macros.view/run-command!
                                  'goto this params reads))
                      show-foo (fn [params & reads]
                                 (workflo.macros.view/run-command!
                                  'show-foo this params reads))]
                  (foo {:on-click #(goto 'some-screen {:id 1})}))))
            (def view
              (workflo.macros.view/factory View {}))
            (workflo.macros.view/register-view!
             'View {:view View :factory view})))))

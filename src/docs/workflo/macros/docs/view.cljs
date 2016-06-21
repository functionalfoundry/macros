(ns workflo.macros.docs.view
  (:require [cljs.pprint :refer [pprint]]
            [devcards.core :refer-macros [defcard defcard-doc dom-node]]
            [om.next :as om]
            [om.dom :as dom]
            [workflo.macros.view :as view :refer-macros [defview]]))

(defcard-doc
  "# `defview` macro")

;;;; Example state with two users

(def users-state
  {:users [{:user/name "Jeff"
            :user/email "jeff@jeff.org"}
           {:user/name "Joe"
            :user/email "joe@joe.org"}]})

;;;; A minimalistic user parser with only a read function

(defmulti read om/dispatch)

(defmethod read :users
  [{:keys [query state]} key _]
  (let [st @state]
    {:value (om/db->tree query (get st key) st)}))

(def users-parser
  (om/parser {:read read :mutate #()}))

(def users-reconciler
  (om/reconciler {:state  users-state
                  :parser users-parser}))

(defcard-doc
  "# Example: Users

  The User view renders a user and triggers a callback function
  when the user is clicked.

  ```
  (defview User
    [user [name email]]
    [clicked-fn]
    (key name)
    (ident [:user name])
    (render
      (dom/div #js {:onClick #(clicked-fn name)}
        (dom/p nil \"Name: \" name)
        (dom/p nil \"Email: \" email))))
  ```

  The UserList view renders multiple children, exercising the
  `:wrapper-view` feature that automatically wraps the content
  of `(render ...)` if it does not return a single element.

  The UserList view also demonstrates that, with views, the
  properties argument is optional: if no props are being used,
  no `nil` props need to be passed in:

  ```
  (defview UserList
    [{users User}]
    (render
     (header {:text \"First user\"})
     (user (first users))
     (header {:text \"Second user\"})
     (user (second users))
     (header {:text \"All users\"})
     (for [u users]
       (user u))))
  ```")

(defview User
  [user [name email]]
  [clicked-fn]
  (key name)
  (ident [:user name])
  (render
   (dom/div #js {:onClick #(clicked-fn name)}
     (dom/p nil "Name: " name)
     (dom/p nil "Email: " email))))

(defview Header
  [text]
  (render
    (dom/h3 nil text)))

(defview Wrapper
  (render
    (dom/section nil (om/children this))))

(view/configure! {:wrapper-view om.dom/article})

(defview UserList
  [{users User}]
  (render
   (header {:text "First user"})
   (user (first users))
   (header {:text "Second user"})
   (user (second users))
   (header {:text "All users"})
   (for [u users]
     (user u))))

(defcard
  "## The output of rendering a UserList with two users:"
  (dom-node
   (fn [_ node]
     (om/add-root! users-reconciler UserList node)))
  users-state)
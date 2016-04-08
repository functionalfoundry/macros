(ns workflo.macros.docs.view
  (:require [cljs.pprint :refer [pprint]]
            [devcards.core :refer-macros [defcard defcard-doc dom-node]]
            [om.next :as om]
            [om.dom :as dom]
            [workflo.macros.view :refer-macros [defview]]))

(defcard-doc
  "# `defview` macro")

(defcard-doc
  "# Example: User

  ```
  (defview User
    [user [name email]]
    [clicked-fn]
    (key name)
    (ident [:user name])
    (render
      (dom/div nil
        (dom/p nil \"Name: \" name)
        (dom/p nil \"Email: \" email))))
  ```")

(def user-state
  (atom {:user/name "Jeff"
         :user/email "jeff@jeff.org"}))

(defn user-read
  [{:keys [state]} key _]
  {:value (key @state)})

(def user-parser
  (om/parser {:read user-read :mutate #()}))

(def user-reconciler
  (om/reconciler {:state user-state :parser user-parser}))

(defview User
  [user [name email]]
  (key name)
  (ident [:user name])
  (render
   (dom/div nil
     (dom/p nil "Name: " name)
     (dom/p nil "Email: " email))))

(defcard
  (dom-node
   (fn [_ node]
     (om/add-root! user-reconciler User node)))
  user-state)

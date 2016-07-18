(ns workflo.macros.docs.screen
  (:require [cljs.pprint :refer [pprint]]
            [devcards.core :refer-macros [defcard defcard-doc dom-node]]
            [workflo.macros.screen :as screen
             :refer-macros [defscreen]]
            [workflo.macros.view :as view
             :refer-macros [defview]]))

(defcard-doc
  "# `defscreen` macro")

(defview UserList
  (render))

(defview UserProfile
  (render))

(defscreen users
  (url "users")
  (navigation
   (title "Users"))
  (layout
   {:content user-list}))

(defscreen user
  (url "user/:user-id")
  (navigation
   (title "User"))
  (layout
   {:content user-profile}))

(defcard
  "## Registered screens

   ```
   (defscreen users
     (url \"users\")
     (navigation
      (title \"Users\"))
     (layout
      {:content user-list}))

   (defscreen user
     (url \"user\")
     (navigation
      (title \"User\"))
     (layout
      {:content user-profile}))
   ```"
  (screen/registered-screens))

(ns workflo.macros.docs.screen
  (:require [bidi.bidi :as bidi]
            [cljs.pprint :refer [pprint]]
            [devcards.core :refer-macros [defcard defcard-doc dom-node]]
            [workflo.macros.screen :as screen :refer [defscreen]]
            [workflo.macros.screen.bidi :as screen-bidi]
            [workflo.macros.view :as view :refer [defview]]))

(defcard-doc
  "# `defscreen` macro")

(defview UserList
  (render))

(defview UserProfile
  (render))

(defview UserSettings
  (render))

(defscreen users
  (url "users")
  (navigation
   {:title "Users"})
  (sections
   {:content user-list}))

(defscreen user
  (url "users/:user-id")
  (navigation
   {:title "User"})
  (sections
   {:content user-profile}))

(defscreen user-settings
  (url "users/:user-id/settings")
  (navigation
   {:title "User Settings"})
  (sections
   {:content user-settings}))

(defcard
  "## Registered screens

   ```
   (defscreen users
     (url \"users\")
     (navigation
      (title \"Users\"))
     (sections
      {:content user-list}))

   (defscreen user
     (url \"user\")
     (navigation
      (title \"User\"))
     (sections
      {:content user-profile}))

   (defscreen user-settings
     (url \"users/:user-id/settings\")
     (navigation
      (title \"User Settings\"))
     (sections
      {:content user-settings}))
   ```"
  (screen/registered-screens))

(defcard
  "## Resolved `users` screen"
  (screen/resolve-screen 'users))

(defcard
  "## Routes

  ```
  (workflo.macros.screen.bidi/routes)
  ```"
  (screen-bidi/routes))

(defcard
  "## Routing for users

   ```
   [(bidi/path-for <routes> 'users)
    (bidi/match-route <routes> \"/users\")
    (workflo.macros.screen.bidi/match \"/users\")]
   ```"
  [(bidi/path-for (screen-bidi/routes) 'users)
   (bidi/match-route (screen-bidi/routes) "/users")
   (screen-bidi/match "/users")])

(defcard
  "## Routing for a user

   ```
   [(bidi/path-for <routes> 'user :user-id 5)
    (bidi/match-route <routes> \"/users/10\")
    (workflo.macros.screen.bidi/match \"/users/10\")]
   ```"
  [(bidi/path-for (screen-bidi/routes) 'user :user-id 5)
   (bidi/match-route (screen-bidi/routes) "/users/10")
   (screen-bidi/match "/users/10")])

(defcard
  "## Routing for a user's settings

   ```
   [(bidi/path-for <routes> 'user-settings :user-id 5)
    (bidi/match-route <routes> \"/users/15/settings\")
    (workflo.macros.screen.bidi/match \"/users/15/settings\")]
   ```"
  [(bidi/path-for (screen-bidi/routes) 'user-settings :user-id 5)
   (bidi/match-route (screen-bidi/routes) "/users/15/settings")
   (screen-bidi/match "/users/15/settings")])

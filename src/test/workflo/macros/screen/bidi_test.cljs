(ns workflo.macros.screen.bidi-test
  (:require [cljs.test :refer-macros [deftest is]]
            [workflo.macros.screen :as screen :refer-macros [defscreen]]
            [workflo.macros.screen.bidi :as screen-bidi]))

(deftest routes
  (def user-view :user-view)
  (def user-settings-view :user-settings-view)
  (def users-view :users-view)
  (defscreen user
    (url "users/:user-id")
    (navigation
      (title "User"))
    (layout
      {:content user-view}))
  (defscreen user-settings
    (url "users/:user-id/settings")
    (navigation
      (title "User Settings"))
    (layout
      {:content user-settings-view}))
  (defscreen users
    (url "users")
    (navigation
      (title "Users"))
    (layout
      {:content users-view}))
  (and
   ;; Check routes generation
   (is (= '["/" [[["users"] users]
                [["users" "/" :user-id] user]
                [["users" "/" :user-id "/" "settings"] user-settings]]]
          (screen-bidi/routes)))
   ;; Check route matching
   (is (= {:screen (screen/resolve-screen 'users)
           :params {}}
          (screen-bidi/match "/users")))
   (is (= {:screen (screen/resolve-screen 'user)
           :params {:user-id "10"}}
          (screen-bidi/match "/users/10")))
   (is (= {:screen (screen/resolve-screen 'user-settings)
           :params {:user-id "15"}}
          (screen-bidi/match "/users/15/settings")))
   ;; Check path generation from screens
   (is (= "/users" (screen-bidi/path 'users)))
   (is (= "/users" (screen-bidi/path (screen/resolve-screen 'users))))
   (is (= "/users/1" (screen-bidi/path 'user :user-id 1)))
   (is (= "/users/1" (screen-bidi/path (screen/resolve-screen 'user)
                                       :user-id 1)))
   (is (= "/users/1/settings" (screen-bidi/path 'user-settings
                                                :user-id 1)))))

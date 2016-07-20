(ns workflo.macros.examples.screen-router
  (:require [cljs.pprint]
            [goog.dom :as gdom]
            [om.next :as om]
            [om.dom :as dom]
            [workflo.macros.screen :refer-macros [defscreen]]
            [workflo.macros.screen.om-next :as so]
            [workflo.macros.view :refer-macros [defview]]))

;;;; Setup

(enable-console-print!)

;;;; Views

(defview UserSettingsView
  [db [id] user [name email]]
  (render
   (dom/div nil
     (dom/h3 nil "User Settings")
     (dom/input #js {:type "text" :value name})
     (dom/input #js {:type "text" :value email}))))

(defview UserView
  [db [id] user [name email]]
  (render
   (dom/div nil
     (dom/h3 nil (str id ": " name))
     (dom/p nil email)
     (dom/p nil
       (dom/a #js {:href (str "#/users/" id)}
         "View")
       " / "
       (dom/a #js {:href (str "#/users/" id "/settings")}
         "Settings")))))

(defview UserSettingsIcon
  (render
   (dom/img #js {:src "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/PICOL_icon_Settings.svg/32px-PICOL_icon_Settings.svg.png"})))

(defview UserSettings
  [({user UserView} {db/id '?user-id})]
  (render
   (user-settings-view user)))

(defview UserIcon
  (render
   (dom/img #js {:src "https://upload.wikimedia.org/wikipedia/commons/thumb/1/12/User_icon_2.svg/48px-User_icon_2.svg.png"})))

(defview UserProfile
  [({user UserView} {db/id '?user-id})]
  (render
   (user-view user)))

(defview UserListIcon
  (render
   (dom/img #js {:src "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1d/Gnome-system-users.svg/48px-Gnome-system-users.svg.png"})))

(defview UserList
  [{users UserView}]
  (render
   (dom/div nil
     (dom/h2 nil "Users")
     (for [u users]
       (user-view u)))))

;;;; Screens

(defscreen UserSettingsScreen
  (url "users/:user-id/settings")
  (navigation
   (title "User Settings"))
  (layout
   {:icon {:view UserSettingsIcon :factory user-settings-icon}
    :content {:view UserSettings :factory user-settings}}))

(defscreen UserScreen
  (url "users/:user-id")
  (navigation
   (title "User"))
  (layout
   {:icon {:view UserIcon :factory user-icon}
    :content {:view UserProfile :factory user-profile}}))

(defscreen UserListScreen
  (url "users")
  (navigation
    (title "Users"))
  (layout
   {:icon {:view UserListIcon :factory user-list-icon}
    :content {:view UserList :factory user-list}}))

;;;; Data

(def initial-state
  {:users [{:db/id 1
            :user/name "John"
            :user/email "john@email.org"}
           {:db/id 2
            :user/name "Jeff"
            :user/email "jeff@email.org"}
           {:db/id 3
            :user/name "Linda"
            :user/email "linda@email.org"}]})

;;;; Om Next

(defmulti read om/dispatch)

(defmethod read :user
  [{:keys [query state]} key params]
  (let [st   @state
        user (first (filter #(= (js/parseInt (:db/id params))
                                (:db/id %))
                            (:users st)))]
    {:value user}))

(defmethod read :users
  [{:keys [state]} key params]
  (let [users (get @state key)]
    {:value users}))

(defmulti mutate om/dispatch)

(defmethod mutate :default
  [env key params]
  (println "MUTATE" key params))

(def parser
  (so/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler {:state initial-state
                  :parser parser}))

;;;; Example app

(defview App
  [navigation layout]
  (render
   (do
     (dom/div nil
       (dom/h1 #js {:style #js {:transition "all 0.5s ease-in"}}
         (:title navigation))
       (dom/table nil
         (dom/thead nil
           (dom/tr nil
             (dom/th nil "Icon")
             (dom/th nil "Content")))
         (dom/tbody nil
           (dom/tr nil
             (dom/td #js {:style #js {:verticalAlign "top"}}
               (:icon layout))
             (dom/td #js {:style #js {:verticalAlign "top"}}
               (:content layout)))))))))

;;;; Bootstrapping

(defonce application (atom nil))

(defn init
  []
  (let [target (gdom/getElement "app")
        app    (so/application {:reconciler reconciler
                                :target target
                                :root app
                                :root-js? false
                                :default-screen 'UserListScreen})]
    (reset! application app)
    (so/start app)))

(defn reload
  []
  (so/reload @application))

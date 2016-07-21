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
     (dom/input #js {:type "text" :value name})
     (dom/input #js {:type "text" :value email}))))

(defview UserView
  [db [id] user [name email]]
  (render
   (dom/div nil
     (dom/p nil (str name " <" email ">"))
     (dom/p nil
       (dom/a #js {:href (str "#/users/" id)}
         "View")
       " / "
       (dom/a #js {:href (str "#/users/" id "/settings")}
         "Settings")))))

(defview UserSettingsTitle
  [({user [db [id] user [name]]} {db/id ?user-id})]
  (render
   (dom/h2 nil (str "Settings for "
                    (:db/id user) ": "
                    (:user/name user)))))

(defview UserSettings
  [({user UserView} {db/id ?user-id})]
  (render
   (user-settings-view user)))

(defview UserTitle
  [({user [user [name]]} {db/id ?user-id})]
  (render
   (dom/h2 nil (str "User "
                    (:db/id user) ": "
                    (:user/name user)))))

(defview UserProfile
  [({user UserView} {db/id ?user-id})]
  (render
   (user-view user)))

(defview UserListTitle
  (render
   (dom/h2 nil "User List")))

(defview UserList
  [{users UserView}]
  (render
   (dom/div nil
     (for [u users]
       (user-view u)))))

;;;; Screens

(defscreen UserSettingsScreen
  (url "users/:user-id/settings")
  (navigation
   (title "User Settings"))
  (layout
   {:title {:view UserSettingsTitle :factory user-settings-title}
    :content {:view UserSettings :factory user-settings}}))

(defscreen UserScreen
  (url "users/:user-id")
  (navigation
   (title "User"))
  (layout
   {:title {:view UserTitle :factory user-title}
    :content {:view UserProfile :factory user-profile}}))

(defscreen UserListScreen
  (url "users")
  (navigation
    (title "Users"))
  (layout
   {:title {:view UserListTitle :factory user-list-title}
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

(defview Block
  [title]
  (key title)
  (render
   (dom/div #js {:style #js {:background "#fafafa"
                             :border "thin solid #ddd"
                             :padding "0.5rem"
                             :marginBottom "1rem"}}
     (dom/p #js {:style #js {:borderBottom "thin solid #ddd"}}
       (dom/strong nil title))
     (om/children this))))

(defview App
  [navigation layout]
  (render
   (do
     (dom/div nil
       (block {:title "Screen"}
         (:title navigation))
       (block {:title "Title"}
         (:title layout))
       (block {:title "Content"}
         (:content layout))))))

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

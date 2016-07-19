(ns workflo.macros.examples.screen-router
  (:require [goog.dom :as gdom]
            [om.next :as om]
            [om.dom :as dom]
            [workflo.macros.screen :refer-macros [defscreen]]
            [workflo.macros.screen.bidi :as sb]
            [workflo.macros.view :refer-macros [defview]]))

;;;; Setup

(enable-console-print!)

;;;; Views

(defview UserSettingsView
  [db [id] user [name email]]
  (render
   (dom/div nil
     (dom/h3 nil "User Settings")
     (dom/input #js {:type :text} name)
     (dom/input #js {:type :text} email))))

(defview UserView
  [db [id] user [name email]]
  (render
   (dom/div nil
            (dom/h3 nil (str id ": " name))
            (dom/p nil email))))

(defview UserProfileView
  [({user UserView} {db/id '?user-id})]
  (render
   (user-view user)))

(defview UserListView
  [{users UserView}]
  (render
   (dom/div nil
     (dom/h2 nil "Users")
     (for [u users]
       (user-view u)))))

;;;; Screens

(defscreen UserListScreen
  (url "users")
  (navigation
    (title "Users"))
  (layout
    {:content {:view UserListView
               :factory user-list-view}}))

(defscreen UserScreen
  (url "users/:id")
  (navigation
    (title "User"))
  (layout
    {:content {:view UserProfileView
               :factory user-profile-view}}))

(defscreen UserSettingsScreen
  (url "users/:id/settings")
  (navigation
    (title "User Settings"))
  (layout
    {:content {:view UserSettingsView
               :factory user-settings-view}}))

;;;; Main app

(defview App
  [navigation layout [content]]
  (render
   (if (:factory content)
     (let [{:keys [factory view props]} content]
       (println content)
       (println "FACTORY" factory view)
       (dom/div nil
         (dom/h1 nil (str "Screen: " (:title navigation)))
         (factory props)))
     (dom/div nil "Not found"))))

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

(defmethod read :navigation
  [{:keys [state]} _ _]
  {:value (:navigation (:screen @state))})

(defmethod read :layout/content
  [{:keys [query parser state] :as env} key params]
  (let [st    @state
        value {:value {:navigation   (-> st :screen :navigation)
                       :view         (-> st :screen :layout
                                         (get (keyword (name key)))
                                         :view)
                       :factory      (-> st :screen :layout
                                         (get (keyword (name key)))
                                         :factory)
                       :props        (parser env query params)}}]
    (println "VALUE" value)
    value))

(defmethod read :users
  [{:keys [state]} key params]
  {:value (get @state key)})

(defmulti mutate om/dispatch)

(defmethod mutate :default
  [env key params]
  (println "MUTATE" key params))

(def parser
  (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler {:state initial-state
                  :parser parser}))

;;;; Routing

(defn mount-screen
  [screen params]
  (println "Mount screen" screen params)
  (let [app     (om/app-root reconciler)
        content (mapv (fn [[k v]]
                        (println "VIEW" (:view v))
                        (println "Q" (om/get-query (:view v)))
                        {(keyword "layout" (name k))
                         (om/get-query (:view v))})
                      (:layout screen))
        query   (into [:navigation] content)]
    (println "QUERY" query)
    (swap! (om/app-state reconciler) assoc :screen screen)
    (om/set-query! app {:params params :query query})))

;;;; Application launch

(defn init
  []
  (let [target (gdom/getElement "app")]
    (om/add-root! reconciler App target))
    (sb/router {:default-screen 'UserListScreen
                :mount-screen mount-screen}))

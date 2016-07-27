(ns workflo.macros.examples.screen-app
  (:require [cljs.pprint]
            [cljs.spec :as s]
            [com.stuartsierra.component :as component]
            [datascript.core :as d]
            [goog.dom :as gdom]
            [om.next :as om]
            [om.dom :as dom]
            [workflo.macros.command :as c :refer-macros [defcommand]]
            [workflo.macros.screen :refer-macros [defscreen]]
            [workflo.macros.screen.bidi :as sb]
            [workflo.macros.screen.om-next :as so]
            [workflo.macros.view :refer-macros [defview]]))

;;;; Setup

(enable-console-print!)

(defonce application
  (atom nil))

;;;; Om Next + DataScript

(defonce initial-users
  [{:db/id 1 :user/name "John" :user/email "john@email.org"}
   {:db/id 2 :user/name "Jeff" :user/email "jeff@email.org"}
   {:db/id 3 :user/name "Linda" :user/email "linda@email.org"}])

(def ds-conn
  (let [conn (d/create-conn {})]
    (d/transact! conn initial-users)
    conn))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :users
  [{:keys [query state] :as env} key params]
  {:value (->> (d/q '[:find [(pull ?u [*]) ...]
                      :in $
                      :where [?u :user/name]]
                    @state)
               (mapv #(select-keys % query)))})

(defmethod read :user
  [{:keys [query state]} key params]
  (let [id (js/parseInt (:db/id params))]
    {:value (-> (d/pull @state '[*] id)
                (select-keys query))}))

(defmethod read :db/id
  [{:keys [query query-root state]} key params]
  {:value (-> (d/pull @state '[*] (second query-root))
              (select-keys query))})

(defmethod mutate :default
  [env key params]
  {:action #(c/run-command key params)})

(def parser
  (so/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler {:state ds-conn
                  :pathopt true
                  :parser parser}))

;;;; Views

(defview UserSettingsView
  [db [id] user [name email]]
  (commands [update-user])
  (.update [this attr e]
    (update-user (merge {:db/id id
                         :user/name name
                         :user/email email}
                        {attr (.. e -target -value)})))
  (render
   (dom/form nil
     (dom/p nil
       (dom/label nil "Name:")
       (dom/input #js {:type :text :value (or name "")
                       :onChange #(.update this :user/name %)}))
     (dom/p nil
       (dom/label nil "Email:")
       (dom/input #js {:type :text :value (or email "")
                       :onChange #(.update this :user/email %)})))))

(defview UserView
  [db [id] user [name email]]
  (commands [goto])
  (render
   (dom/div nil
     (dom/p nil (str name " <" email ">"))
     (dom/p nil
       (dom/button #js {:onClick #(goto {:screen 'UserScreen
                                         :params {:user-id id}})}
         "View")
       (dom/button #js {:onClick #(goto {:screen 'UserSettingsScreen
                                         :params {:user-id id}})}
         "Settings")))))

(defview UserSettingsTitleView
  [db [id] user [name]]
  (render
   (dom/h2 nil (str "Settings for " id ": " name))))

(defview UserSettingsTitle
  [({user UserSettingsTitleView} {db/id ?user-id})]
  (render
   (user-settings-title-view user)))

(defview UserSettings
  [({user UserSettingsView} {db/id ?user-id})]
  (key (:db/id (:user props)))
  (render
   (user-settings-view user)))

(defview UserTitleView
  [db [id] user [name]]
  (render
   (dom/h2 nil (str "User " id ": " name))))

(defview UserTitle
  [({user [user [name]]} {db/id ?user-id})]
  (render
   (user-title-view user)))

(defview UserProfile
  [({user UserView} {db/id ?user-id})]
  (render
   (user-view user)))

(defview UserListTitle
  (render
   (dom/h2 nil "User List")))

(defview UserList
  [{users UserView}]
  (commands [add-user])
  (render
   (dom/div nil
     (for [u users]
       (user-view u))
     (dom/p nil
       (dom/button #js {:onClick #(add-user {:db/id (inc (count users))
                                             :user/name ""
                                             :user/email ""})}
         "Add user")))))

;;;; Screens

(defscreen UserSettingsScreen
 (url "users/:user-id/settings")
 (navigation
  {:title "User Settings"})
 (layout
  {:title {:view UserSettingsTitle :factory user-settings-title}
   :content {:view UserSettings :factory user-settings}}))

(defscreen UserScreen
  (url "users/:user-id")
  (navigation
   {:title "User"})
  (layout
   {:title {:view UserTitle :factory user-title}
    :content {:view UserProfile :factory user-profile}}))

(defscreen UserListScreen
  (url "users")
  (navigation
    {:title "Users"})
  (layout
   {:title {:view UserListTitle :factory user-list-title}
    :content {:view UserList :factory user-list}}))

;;;; Commands

(s/def :db/id (s/and int? pos?))
(s/def :user/name (s/or :nil nil? :string string?))
(s/def :user/email (s/or :nil nil? :string string?))
(s/def ::user
  (s/keys :req [:db/id]
          :opt [:user/name
                :user/email]))

(defcommand add-user
  [::user]
  (do
    (println "add-user" data)
    {:state {(:db/id data) data}
     :location {:screen 'UserSettingsScreen
                :params {:user-id (:db/id data)}}}))

(defcommand update-user
  [::user]
  (do
    (println "update-user" data)
    {:state {(:db/id data) data}}))

(s/def ::screen symbol?)
(s/def ::params map?)
(s/def ::location
  (s/keys :req-un [::screen ::params]))

(defcommand goto
 [::location]
 (do
   (println "goto" data)
   {:location data}))

(defn process-command-result
  [{:keys [state location]}]
  (when state
    (d/transact! (om/app-state reconciler) (vals state))
    (cljs.pprint/pprint (om/app-state reconciler)))
  (when location
    (so/goto @application
             (:screen location)
             (:params location))))

(c/configure-commands! {:process-result process-command-result})

;;;;;; Example app

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
  (render
   (let [{:keys [navigation layout]} (om/props this)]
     (dom/div nil
       (block {:title "Screen"}
         (:title navigation))
       (block {:title "Title"}
         (:title layout))
       (block {:title "Content"}
         (:content layout))))))

;;;; Bootstrapping

(defn init []
  (->> (so/application {:reconciler reconciler
                        :target (gdom/getElement "app")
                        :root app
                        :root-js? false
                        :default-screen 'UserListScreen
                        :screen-mounted
                        (fn [app screen params]
                          (println "Screen mounted:" (:name screen))
                          (println "Query:")
                          (cljs.pprint/pprint (-> reconciler
                                                  om/app-root
                                                  om/get-query)))})
       (component/start)
       (reset! application)))


(defn reload []
  (so/reload @application))

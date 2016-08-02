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
            [workflo.macros.service :as service
             :refer-macros [defservice]]
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
  {:action #(c/run-command! key params)})

(def parser
  (so/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler {:state ds-conn
                  :pathopt true
                  :parser parser}))

;;;; Views

(defview UserSettingsView
  (query [db [id] user [name email]])
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
  (query [db [id] user [name email]])
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
  (query [db [id] user [name]])
  (render
   (dom/h2 nil (str "Settings for " id ": " name))))

(defview UserSettingsTitle
  (query [({user UserSettingsTitleView} {db/id ?user-id})])
  (render
   (user-settings-title-view user)))

(defview UserSettings
  (query [({user UserSettingsView} {db/id ?user-id})])
  (key (:db/id (:user props)))
  (render
   (user-settings-view user)))

(defview UserTitleView
  (query [db [id] user [name]])
  (render
   (dom/h2 nil (str "User " id ": " name))))

(defview UserTitle
  (query [({user [user [name]]} {db/id ?user-id})])
  (render
   (user-title-view user)))

(defview UserProfile
  (query [({user UserView} {db/id ?user-id})])
  (render
   (user-view user)))

(defview UserListTitle
  (render
   (dom/h2 nil "User List")))

(defview UserList
  (query [{users UserView}])
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
  {:title 'UserSettingsTitle
   :content 'UserSettings}))

(defscreen UserScreen
  (url "users/:user-id")
  (navigation
   {:title "User"})
  (layout
   {:title 'UserTitle
    :content 'UserProfile}))

(defscreen UserListScreen
  (url "users")
  (navigation
    {:title "Users"})
  (layout
   {:title 'UserListTitle
    :content 'UserList}))

;;;; Commands

(s/def :db/id (s/and int? pos?))
(s/def :user/name (s/or :nil nil? :string string?))
(s/def :user/email (s/or :nil nil? :string string?))
(s/def ::user
  (s/keys :req [:db/id]
          :opt [:user/name
                :user/email]))

(defcommand add-user
  (spec ::user)
  (emit
    (println "add-user" data)
    {:state {(:db/id data) data}
     :location {:screen 'UserSettingsScreen
                :params {:user-id (:db/id data)}}}))

(defcommand update-user
  (spec ::user)
  (emit
    (println "update-user" data)
    {:state {(:db/id data) data}}))

(s/def ::screen symbol?)
(s/def ::params map?)
(s/def ::location
  (s/keys :req-un [::screen ::params]))

(defcommand goto
  (spec ::location)
  (emit
   (println "goto" data)
   {:location data}))

;;;; Services

(defservice state
  (process
   (d/transact! (om/app-state reconciler) (vals data))
   (cljs.pprint/pprint (om/app-state reconciler))))

(defservice location
  (process
   (so/goto @application (:screen data) (:params data))))

(c/configure-commands! {:process-emit service/deliver-to-services!})

;;;;;; Example app

(defview Block
  (query [title])
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
  (-> 'location service/new-service-component component/start)
  (-> 'state service/new-service-component component/start)
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

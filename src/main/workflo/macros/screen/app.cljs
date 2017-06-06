(ns workflo.macros.screen.app
  (:require [com.stuartsierra.component :as component]
            [om.dom :as dom]
            [workflo.macros.query.om-next :as om-query]
            [workflo.macros.screen.bidi :as sb]
            [workflo.macros.view :as views :refer [defview]]))

(defview RootWrapper
  (query
    [workflo [screen forms sections]])
  (render
    (do
      (js/console.warn "RENDER ROOT")
      (js/console.warn "SCREEN" (clj->js screen))
      (js/console.warn "FORMS" (clj->js forms))
      (js/console.warn "SECTIONS" (clj->js sections))
      (js/React.createElement "div" nil "Root"))))

(defn screen-query [screen]
  (into {}
    (map (fn [[section-name view-name]]
           (let [view (views/resolve-view view-name)]
             (js/console.warn "VIEW" (clj->js view))
             [section-name (om-query/query (:query view))])))
    (get screen :sections)))

(defn realize-root [app screen params]
  (let [query        (screen-query screen)
        query-fn     (get-in app [:config :query])
        query-result (query-fn query {:params params})]
    (js/console.warn "QUERY" (clj->js query))
    (js/console.warn "QUERY RESULT" (clj->js query-result))
    (js/ReactDOM.render (root-wrapper query-result)
                        (get-in app [:config :target]))))

(defn mount-screen [app screen params]
  (realize-root app screen params))

(defprotocol IApp
  (render [this])
  (reload [this])
  (goto [this screen params]))

(defrecord App [config router state]
  component/Lifecycle
  (start [this]
    (render this)
    (assoc this :router
      (sb/router {:default-screen (get config :default-screen)
                  :mount-screen (partial mount-screen this)})))
  
  (stop [this]
    (dissoc this :router))
    
  IApp
  (render [this]
    (mount-screen this
                  (get config :default-screen)
                  (get config :default-params)))
    
  (reload [this])
  
  (goto [this screen params]))

(defn app [config]
  (map->App {:config config
             :state  (atom {})}))

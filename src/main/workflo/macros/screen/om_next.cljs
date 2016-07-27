(ns workflo.macros.screen.om-next
  (:require [clojure.walk :refer [walk]]
            [com.stuartsierra.component :as component]
            [om.next :as om]
            [workflo.macros.screen.bidi :as sb]
            [workflo.macros.screen :as scr]
            [workflo.macros.util.string :refer [kebab->camel]]
            [workflo.macros.view
             :refer [resolve-view]
             :refer-macros [defview]]))

;;;; Remember the active screen

(defonce ^:private +active-screen+
  (atom {:screen nil :params nil}))

(defn active-screen
  "Returns the active screen as a map, with :screen storing
   the current screen and :params storing the URL parameters
   for the current screen."
  []
  @+active-screen+)

(defn set-active-screen!
  "Sets the active screen and its parameters."
  [screen params]
  (swap! +active-screen+ assoc
         :screen screen
         :params params))

(defn reload-active-screen!
  "Reloads the implementation of the active screen by replacing
   it with a freshly resolved instance from the screen registry."
  []
  (swap! +active-screen+ update :screen
         (fn [screen]
           (some-> screen :name scr/resolve-screen))))

;;;; Wrapping Om Next parser to handle screen-based routing

(defn read-screen
  "Executes a query agains the active screen."
  [read {:keys [parser query] :as env} _ _]
  {:value (parser (assoc env :screen (:screen (active-screen)))
                  query nil)})

(defn read-forms
  "Queries the forms of the active screen."
  [_ {:keys [screen]} _ _]
  {:value (:forms screen)})

(defn read-layout
  "Executes all queries for views in the layout of the
   active screen."
  [_ {:keys [parser query screen] :as env} _ _]
  {:value
   (reduce (fn [res layout-item]
             (let [[section query] (first layout-item)]
               (assoc res section
                      {:view  (-> screen :layout section
                                  resolve-view :factory)
                       :props (parser (dissoc env :path) query nil)})))
           {} query)})

(defn- wrapping-read
  "Wraps a user-provided parser read function for Om Next by
   special-casing queries for screen-based routing information
   (such as :workflo/screen, :workflo/forms and
   :workflo/layout.)"
  [read env key params]
  (case key
    :workflo/screen (read-screen read env key params)
    :workflo/forms  (read-forms read env key params)
    :workflo/layout (read-layout read env key params)
    (read env key params)))

(defn parser
  "Returns a parser that wraps the provided read function to
   catch queries for screen-based routing information."
  [{:keys [read mutate]}]
  (om/parser {:read (partial wrapping-read read)
              :mutate mutate}))

;;;; Root component

(defonce ^:private +root-component+
  (atom {:factory nil :js? false}))

(defn root-component
  "Returns the root component to be used."
  []
  @+root-component+)

(defn set-root-component!
  "Configures the root component to be used."
  [factory js?]
  (swap! +root-component+ assoc
         :factory factory
         :js? js?))

;;;; Wrapping application

(defn- realize-layout
  "Realizes the results of a layout query by instantiating
   all returned views with their props / query results."
  [layout]
  (zipmap (keys layout)
          (map (fn [{:keys [view props] :as item}]
                 (view (vary-meta props assoc :om-path
                                  (-> item meta :om-path))))
               (vals layout))))

(defn camel-cased-prop-map
  "Convert a Clojure map with Om Next properties to a map
   where all keys are camel-cased strings that can be accessed
   like object properties in JS/React."
  [m]
  (walk (fn [[k v]]
          [(kebab->camel (name k))
           (cond-> v (map? v) camel-cased-prop-map)])
        identity
        m))

(defview RootWrapper
  [{workflo/screen [workflo [forms layout]]}]
  (render
   (let [forms      (:workflo/forms screen)
         layout     (realize-layout (:workflo/layout screen))
         root-info  (root-component)]
     ((:factory root-info)
      (if (:js? root-info)
        (do
          (clj->js (merge {:layout (camel-cased-prop-map layout)}
                          (camel-cased-prop-map forms))))
        (merge {:layout layout} forms))))))

;;;; Routing

(defn mount-screen
  "Mounts a screen with parameters by setting it as the active
   screen and updating the root wrapper's component query
   according to the screen's layout."
  [app screen params]
  (let [c     (om/app-root (:reconciler (:config app)))
        query [{:workflo/screen
                [:workflo/forms
                 {:workflo/layout
                  (mapv (fn [[section view-name]]
                          {section (or (some-> view-name resolve-view
                                               :view om/get-query)
                                       [])})
                        (:layout screen))}]}]]
    (set-active-screen! screen params)
    (om/set-query! c {:params params :query query})
    (some-> app :config :screen-mounted
            (apply [app screen params]))))

;;;; Application bootstrapping

(defprotocol IApplication
  (mount [this])
  (reload [this])
  (goto [this screen params]))

(defrecord Application [config router]
  component/Lifecycle
  (start [this]
    (mount this)
    (assoc this :router
           (sb/router
            {:default-screen (:default-screen config)
             :mount-screen (partial mount-screen this)})))

  (stop [this]
    (dissoc this :router))

  IApplication
  (mount [this]
    (om/add-root! (:reconciler config) RootWrapper (:target config)))

  (reload [this]
    (reload-active-screen!)
    (mount this)
    (let [{:keys [screen params]} (active-screen)]
      (mount-screen this screen params)))

  (goto [this screen params]
    (sb/goto! router screen params)))

(defn application
  "Creates an Om Next application that implements screen-based
   routing. Takes an Om Next reconciler, a default screen, a
   target DOM element, a root component and a flag as to whether
   or not the root component is a JS component.

   The root component is expected to accept the properties
   defined for screens, so `:layout` and arbitrary other forms.
   `:layout` is a map of layout keys to instantiated components.
   The root component can then decide which of these components
   to render where based on these layout keys.

   During rendering, the provided root component is wrapped
   in an application component that handles the screen routing
   logic and generates the `:layout` and other form props
   based on the active screen."
  [{:keys [default-screen reconciler root
           root-js? target screen-mounted]
    :or   {root-js? false}}]
  (set-root-component! root root-js?)
  (map->Application {:config {:default-screen default-screen
                              :reconciler reconciler
                              :target target
                              :screen-mounted screen-mounted}}))

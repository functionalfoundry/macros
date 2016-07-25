(ns workflo.macros.screen.om-next
  (:require [om.next :as om]
            [workflo.macros.screen.bidi :as sb]
            [workflo.macros.screen :as scr]
            [workflo.macros.view :refer-macros [defview]]))

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

(defn read-navigation
  "Queries the navigation info of the active screen."
  [_ {:keys [screen]} _ _]
  {:value (:navigation screen)})

(defn read-layout
  "Executes all queries for views in the layout of the
   active screen."
  [_ {:keys [parser query screen] :as env} _ _]
  {:value (reduce (fn [res layout-item]
                    (let [[k v] (first layout-item)]
                      (assoc res k {:view (-> screen :layout
                                              k :factory)
                                    :props (parser (dissoc env :path)
                                                   v nil)})))
                  {} query)})

(defn- wrapping-read
  "Wraps a user-provided parser read function for Om Next by
   special-casing queries for screen-based routing information
   (such as :workflo/screen, :workflo/navigation and
   :workflo/layout.)"
  [read env key params]
  (case key
    :workflo/screen     (read-screen read env key params)
    :workflo/navigation (read-navigation read env key params)
    :workflo/layout     (read-layout read env key params)
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
          (map #((:view %) (:props %))
               (vals layout))))

(defview RootWrapper
  [{workflo/screen [workflo [navigation layout]]}]
  (render
   (let [{:keys [workflo/navigation workflo/layout]} screen]
     ((:factory (root-component))
      (if (:js? (root-component))
        #js {:navigation navigation
             :layout (realize-layout layout)}
        {:navigation navigation
         :layout (realize-layout layout)})))))

;;;; Routing

(defn mount-screen
  "Mounts a screen with parameters by setting it as the active
   screen and updating the root wrapper's component query
   according to the screen's layout."
  [app screen params]
  (let [c     (om/app-root (:reconciler (:config app)))
        query [{:workflo/screen
                [:workflo/navigation
                 {:workflo/layout
                  (mapv (fn [[k v]]
                          {k (or (om/get-query (:view v))
                                 [])})
                        (:layout screen))}]}]]
    (set-active-screen! screen params)
    (om/set-query! c {:params params :query query})
    (some-> app :config :screen-mounted
            (apply [app screen params]))))

;;;; Application bootstrapping

(defprotocol IApplication
  (mount [this])
  (start [this])
  (reload [this])
  (goto [this screen params]))

(defrecord Application [config router]
  IApplication
  (mount [this]
    (om/add-root! (:reconciler config) RootWrapper (:target config)))

  (start [this]
    (mount this)
    (assoc this :router
           (sb/router
            {:default-screen (:default-screen config)
             :mount-screen (partial mount-screen this)})))

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

   The root component is expected to accept two properties:
   `:navigation` and `:layout`, where `:navigation` is a map
   of navigation fields defined in the active screen and
   `:layout` is a map of layout keys to instantiated components.
   The root component can then decide which of these components
   to render where based on these layout keys.

   During rendering, the provided root component is wrapped
   in an application component that handles the screen routing
   logic and generates the `:navigation` and `:layout` props
   based on the active screen."
  [{:keys [default-screen reconciler root
           root-js? target screen-mounted]
    :or   {root-js? false}}]
  (set-root-component! root root-js?)
  (map->Application {:config {:default-screen default-screen
                              :reconciler reconciler
                              :target target
                              :screen-mounted screen-mounted}}))

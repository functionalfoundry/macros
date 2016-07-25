(ns workflo.macros.screen.bidi
  (:require [bidi.bidi :as bidi]
            [bidi.router :as br]
            [workflo.macros.screen :as scr]))

(defn route
  "Returns a bidi route for the given screen."
  [[screen-name screen]]
  (let [segments (:segments (:url screen))]
    [(-> segments
         (interleave (repeat "/"))
         (butlast)
         (vec))
     screen-name]))

(defn routes
  "Returns combined bidi routes for all registered screens."
  []
  ["/" (mapv route (scr/registered-screens))])

(defn match-location
  [location]
  {:screen (scr/resolve-screen (-> location :handler name symbol))
   :params (:route-params location)})

(defn match
  "Matches a URL against all screen routes. Returns a
   {:params <route params> :screen <screen>} map, where :screen
   holds the screen for the URL and the route params map all
   parameterizable URL segments (e.g. :user-id) to their values
   in the URL."
  [url]
  (when-let [location (bidi/match-route (routes) url)]
    (match-location location)))

(defn path
  "Returns a URL path for the given screen and the given URL
   parameters. Accepts both screen names and screens. For example,

   (path 'user :user-id 1)
   (path (workflo.macros.screen/resolve-screen 'user) :user-id 1)

   are both acceptable uses of this function."
  [screen-or-name & params]
  (let [screen-name (cond-> screen-or-name
                      (map? screen-or-name)
                      :name)]
    (apply (partial bidi/path-for (routes) screen-name)
           params)))

(defn- on-navigate
  [env location]
  (let [{:keys [screen params]} (match-location location)]
    (some-> env :mount-screen (apply [screen params]))))

(defn router
  [{:keys [default-screen
           mount-screen]
    :or   {default-screen 'home}
    :as   env}]
  (br/start-router! (routes)
                    {:on-navigate #(on-navigate env %)
                     :default-location {:handler default-screen}}))

(defn goto!
  [router screen params]
  (let [location {:handler screen :route-params params}]
    (br/set-location! router location)))

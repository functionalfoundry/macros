(ns workflo.macros.screen.bidi
  (:require [bidi.bidi :as bidi]
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

(defn match
  "Matches a URL against all screen routes. Returns a
   {:params <route params> :screen <screen>} map, where :screen
   holds the screen for the URL and the route params map all
   parameterizable URL segments (e.g. :user-id) to their values
   in the URL."
  [url]
  (when-let [result (bidi/match-route (routes) url)]
    {:params (:route-params result)
     :screen (scr/resolve-screen (-> result :handler name symbol))}))

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

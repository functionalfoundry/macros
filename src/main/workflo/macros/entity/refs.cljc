(ns workflo.macros.entity.refs
  (:require [workflo.macros.entity.schema :as es]))

(def +refmap+ (atom {}))

(defn- add-ref-and-backref!
  [source-entity attr ref-info]
  (let [target-entity (:entity ref-info)]
    (swap! +refmap+ (fn [refmap]
                      (-> refmap
                          (assoc-in [source-entity :refs attr]
                                    {:entity target-entity :many? (:many ref-info)})
                          (assoc-in [target-entity :backrefs attr]
                                    {:entity source-entity :many? true}))))))

(defn remove-backrefs-to
  [entity-name backrefs]
  (let [new-backrefs (into {} (remove (fn [[attr ref-info]]
                                        (= entity-name
                                           (:entity ref-info))))
                           backrefs)]
    (when (seq new-backrefs)
      new-backrefs)))

(defn register-entity-refs!
  "Adds all refs defined by an entity to the intetnal refmap.
   This includes adding backrefs in the reverse direction."
  [entity-name entity-def]
  (when-not (es/simple-entity? entity-def)
    (let [refs (es/entity-refs entity-def)]
      (doseq [[attr ref-info] refs]
        (add-ref-and-backref! entity-name attr ref-info)))))

(defn unregister-entity-refs!
  "Removes all refs defined by an entity from the internal refmap.
   This includes backrefs created for the refs of the entity."
  [entity-name]
  (swap! +refmap+ (fn [refmap]
                    (reduce (fn [out [source-entity refs-and-backrefs]]
                              (conj out [source-entity
                                         (update refs-and-backrefs :backrefs
                                                 (partial remove-backrefs-to entity-name))]))
                            {} (update refmap entity-name dissoc :refs)))))

(defn entity-refs
  "Returns all references from an entity to other entities. The result
   is a map that maps attribute names (e.g. `:user/friends`) to reference
   infos (e.g. `{:entity 'user :many? true}`."
  [entity-name]
  (get-in @+refmap+ [entity-name :refs]))

(defn entity-backrefs
  "Returns all references to an entity from other entities. The result
   is a map that maps attribute names (e.g. `:post/author`) to refernce
   infos (e.g. `{:entity 'post :many? true}`."
  [entity-name]
  (get-in @+refmap+ [entity-name :backrefs]))

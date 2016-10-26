(ns workflo.macros.util.misc)

(defn val-after
  [coll x]
  (loop [coll coll]
    (if (= x (first coll))
      (first (rest coll))
      (when (not (empty? (rest coll)))
        (recur (rest coll))))))

(defn drop-keys
  [m ks]
  (letfn [(drop-kv? [[k v]]
            (some #{k} ks))]
    (into {} (remove drop-kv?) m)))

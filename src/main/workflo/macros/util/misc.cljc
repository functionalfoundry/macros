(ns workflo.macros.util.misc)

(defn val-after
  [coll x]
  (loop [coll coll]
    (if (= x (first coll))
      (first (rest coll))
      (when (not (empty? (rest coll)))
        (recur (rest coll))))))

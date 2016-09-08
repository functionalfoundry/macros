(ns workflo.macros.query.util
  (:require #?(:cljs [cljs.pprint :refer [pprint]]
               :clj  [clojure.pprint :refer [pprint]])
            #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.spec.impl.gen :as gen]
               :clj  [clojure.spec.gen :as gen])
            [clojure.string :refer [capitalize]]))

(s/fdef one-item?
  :args (s/cat :coll coll?)
  :ret  boolean?
  :fn   (s/or :true  (s/and #(= 1 (count (:coll (:args %))))
                            #(true? (:ret %)))
              :false (s/and #(not= 1 (count (:coll (:args %))))
                            #(false? (:ret %)))))

(defn one-item?
  [coll]
  (= 1 (count coll)))

(s/fdef combine-properties-and-groups
  :args (s/cat :props-and-groups vector?)
  :ret  vector?)

(defn combine-properties-and-groups
  "Takes a vector of property names or [base subprops] groups,
   e.g. [foo [bar [baz ruux]]], and returns a flat vector into which
   the group vectors are spliced, e.g. [foo bar [bax ruux]]."
  [props-and-groups]
  (transduce (map (fn [prop-or-group]
                    (cond-> prop-or-group
                      (not (vector? prop-or-group))
                      vector)))
             (comp vec concat)
             []
             props-and-groups))


(s/fdef capitalized-name
  :args (s/cat :x (s/and symbol?
                         #(not (nil? (name %)))))
  :ret string?
  :fn (s/and #(= (first (:ret %))
                 (first (capitalize (name (:x (:args %))))))
             #(= (rest (:ret %))
                 (rest (name (:x (:args %)))))))

(defn capitalized-name
  "Returns the name of a symbol, keyword or string, with the first
   letter capitalized."
  [x]
  (apply str
         (capitalize (first (name x)))
         (rest (name x))))

(s/fdef capitalized-symbol?
  :args (s/cat :x any?)
  :ret boolean?
  :fn (s/or
       :capitalized-symbol
       (s/and #(symbol? (:x (:args %)))
              #(= (first (name (:x (:args %))))
                  (first (capitalize (first (name (:x (:args %)))))))
              #(true? (:ret %)))
       :other
       #(false? (:ret %))))

(defn capitalized-symbol?
  "Returns true if x is a symbol that starts with a capital letter."
  [x]
  (and (symbol? x)
       (= (name x)
          (capitalized-name x))))

(defn print-spec-gen
  "Takes a spec (e.g. as a keyword or symbol) and pretty-prints
   10 random values generated for this spec."
  [spec]
  (println "Values generated from spec" (name spec))
  (try
    (pprint (gen/sample (s/gen spec) 10))
    (catch #?(:cljs js/Object :clj Exception) e
      (println "Error: Spec not found" e))))

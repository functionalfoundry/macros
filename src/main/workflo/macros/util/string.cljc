(ns workflo.macros.util.string
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [clojure.string :as string]))

(s/fdef camel->kebab
  :args (s/cat :s string?)
  :ret string?)

(defn camel->kebab
  "Converts from camel case (e.g. Foo or FooBar) to kebab case
   (e.g. foo or foo-bar)."
  [s]
  (let [segments (re-seq #"[A-Z][a-z0-9_-]*" s)]
    (cond
      segments (->> segments (string/join "-") (string/lower-case))
      :else    s)))

(s/fdef kebab->camel
  :args (s/cat :s string?)
  :ret  string?)

(defn kebab->camel
  "Converts from kebab case (e.g. foo-bar) to camel case (e.g.
   fooBar)."
  [s]
  (let [words (re-seq #"\w+" s)]
    (apply str
           (cons (first words)
                 (map string/capitalize (rest words))))))

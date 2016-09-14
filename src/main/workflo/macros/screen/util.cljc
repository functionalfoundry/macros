(ns workflo.macros.screen.util
  (:require [clojure.spec :as s]
            [clojure.string :as string]))

(s/def ::typed-url-segment
  (s/cat :qualifier '#{long keyword uuid}
         :keyword keyword?))

(s/fdef url-segment
  :args (s/cat :s string?)
  :ret  (s/or :string string?
              :keyword keyword?
              :typed ::typed-url-segment)
  :fn   (s/or :string (s/and #(not= \: (-> % :args :s first))
                             #(= :string (-> % :ret first))
                             #(string? (-> % :ret second)))
              :keyword (s/and #(= \: (-> % :args :s first))
                              #(= :keyword (-> % :ret first))
                              #(keyword? (-> % :ret second)))
              :typed   (s/and #(= \: (-> % :args :s first))
                              #(re-find #"\^" (-> % :args :s))
                              #(= :typed (-> % :ret first))
                              #(vector? (-> % :ret second))
                              #(= 2 (count (-> % :ret second)))
                              #(some #{(-> % :ret second first)}
                                     '[long keyword uuid])
                              #(keyword? (-> % :ret second second)))))

(defn url-segment
  [s]
  (if (= \: (first s))
    (let [[name type] (string/split (subs s 1) #"\^")]
      (if type
        [(symbol type) (keyword name)]
        (keyword name)))
    s))

(s/fdef url-segments
  :args (s/cat :s string?)
  :ret  (s/coll-of (s/or :string string?
                         :keyword keyword?
                         :typed ::typed-url-segment)
                   :kind vector?))

(defn url-segments
  "Returns a vector of URL segments, converting keyword-like
   string segments into actual keywords."
  [s]
  (mapv url-segment (string/split s #"/")))

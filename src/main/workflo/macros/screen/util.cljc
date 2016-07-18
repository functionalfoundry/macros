(ns workflo.macros.screen.util
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            [clojure.string :as string]))

(s/fdef url-segment
  :args (s/cat :s string?)
  :ret  (s/or :string string?
              :keyword keyword?)
  :fn   (s/or :string (s/and #(not= \: (-> % :args :s first))
                             #(= :string (-> % :ret first))
                             #(string? (-> % :ret second)))
              :keyword (s/and #(= \: (-> % :args :s first))
                              #(= :keyword (-> % :ret first))
                              #(keyword? (-> % :ret second)))))

(defn url-segment
  [s]
  (cond-> s
    (= \: (first s)) (-> (subs 1) keyword)))

(s/fdef url-segments
  :args (s/cat :s string?)
  :ret  #?(:cljs (s/and vector?
                        (s/* (s/or :string string?
                                   :keyword keyword?)))
           :clj  (s/coll-of (s/or :string string?
                                  :keyword keyword?)
                            :kind vector?)))

(defn url-segments
  "Returns a vector of URL segments, converting keyword-like
   string segments into actual keywords."
  [s]
  (mapv url-segment (string/split s #"/")))

(ns workflo.macros.specs.types
  (:refer-clojure :exclude [bigdec? bytes? double? float? uri?])
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])))

;;;; Helpers

(defn long? [x]
  #?(:cljs (and (number? x)
                (not (js/isNaN x))
                (not (identical? x js/Infinity))
                (= 0 (rem x 1)))
     :clj  (instance? java.lang.Long x)))

(defn bigint? [x]
  #?(:cljs (long? x)
     :clj  (instance? clojure.lang.BigInt x)))

(defn float? [x]
  #?(:cljs (and (number? x)
                (not (js/isNaN x))
                (not (identical? x js/Infinity))
                (not= (js/parseFloat x) (js/parseInt x 10)))
     :clj  (clojure.core/float? x)))

(defn double? [x]
  #?(:cljs (float? x)
     :clj  (clojure.core/double? x)))

(defn bigdec? [x]
  #?(:cljs (float? x)
     :clj  (clojure.core/bigdec? x)))

(defn bytes? [x]
  #?(:cljs (array? x)
     :clj  (clojure.core/bytes? x)))

;;;; Fundamental types

(s/def ::keyword keyword?)
(s/def ::string string?)
(s/def ::boolean boolean?)
(s/def ::long long?)
(s/def ::bigint bigint?)
(s/def ::float float?)
(s/def ::double double?)
(s/def ::bigdec bigdec?)
(s/def ::instant inst?)
(s/def ::uuid uuid?)
(s/def ::bytes bytes?)

;; TODO What is a URI in ClojureScript? In Clojure there is
;; java.net.URI and a clojure.core/uri? predicate.
;; (s/def ::uri ::TODO)

;; TODO How do we specify allowed enum values?
;; (s/def ::enum ::TODO)

;;;; Entity IDs

;; TODO How do we specify different formats for IDs for
;; DataScript / Datomic / Redis when we at the same time
;; want to transfer entity data from one to the other?
;; Will we need to pass all data through a transformation
;; function that converts IDs (and other things, potentially)?
(s/def ::id ::s/any)

;;;; Reference types

(s/def ::ref ::id)
(s/def ::ref-many
  #?(:cljs (s/and vector? (s/* ::id))
     :clj  (s/coll-of ::id :kind vector?)))

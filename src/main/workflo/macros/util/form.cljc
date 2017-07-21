(ns workflo.macros.util.form
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [workflo.macros.util.symbol]))

(s/fdef prefixed-form-name
  :args (s/cat :form-name :workflo.macros.util.symbol/unqualified-symbol
               :prefix :workflo.macros.util.symbol/unqualified-symbol)
  :ret  symbol?
  :fn   #(= (-> % :ret)
            (symbol (str (-> % :args :prefix) "-"
                         (-> % :args :form-name)))))

(defn prefixed-form-name
  [form-name prefix]
  (symbol (str prefix "-" form-name)))

#?(:clj
   (s/fdef qualified-form-name
     :args (s/cat :form-name
                  :workflo.macros.util.symbol/unqualified-symbol
                  :prefix
                  :workflo.macros.util.symbol/unqualified-symbol)
     :ret  symbol?
     :fn   (s/and #(= (ns-name *ns*) (symbol (namespace (-> % :ret))))
                  #(= (prefixed-form-name (-> % :args :form-name)
                                          (-> % :args :prefix))
                      (symbol (name (-> % :ret)))))))

(defn qualified-form-name
  [form-name prefix]
  (symbol (str (ns-name *ns*))
          (str (prefixed-form-name form-name prefix))))

(s/def ::form-name
  :workflo.macros.util.symbol/unqualified-symbol)

(s/def ::form-body
  any?)

(s/def ::form-args
  vector?)

(s/def ::conforming-form
  (s/with-gen
    (s/keys ::req-un [::form-name ::form-body]
            ::opt-un [::form-args])
    #(gen/hash-map :form-name (s/gen ::form-name)
                   :form-body (s/gen ::form-body))))

(s/def ::conforming-forms
  (s/spec (s/* ::conforming-form)))

(s/fdef forms-map
  :args (s/cat :forms ::conforming-forms
               :prefix :workflo.macros.util.symbol/unqualified-symbol)
  :ret  (s/map-of keyword? symbol?))

(defn forms-map
  [forms prefix]
  (zipmap (map (comp keyword :form-name) forms)
          (map (comp #(qualified-form-name % prefix)
                     :form-name)
               forms)))

(defn make-def
  ([name body]
   `(~'def ~name ~body))
  ([prefix name body]
   `(~'def ~(prefixed-form-name name prefix) ~body)))

(defn make-def-quoted
  ([name body]
   `(~'def ~name '~body))
  ([prefix name body]
   `(~'def ~(prefixed-form-name name prefix) '~body)))

(defn make-defn
  ([name args body]
   `(~'defn ~name ~args
     ~@body))
  ([prefix name args body]
   `(~'defn ~(prefixed-form-name name prefix) ~args
     ~@body)))

(s/fdef form->defn
  :args (s/cat :form ::conforming-form)
  :ret  (s/cat :defn #{'defn}
               :name :workflo.macros.util.symbol/unqualified-symbol
               :body (s/* any?)))

(defn form->defn
  [form]
  (make-defn (:form-name form)
             (:form-args form)
             (:form-body form)))

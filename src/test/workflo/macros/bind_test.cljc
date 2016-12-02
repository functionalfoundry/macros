(ns workflo.macros.bind-test
  (:require [clojure.test :refer [are deftest is]]
            [workflo.macros.bind :refer [with-query-bindings]]))

(deftest regular-properties
  (with-query-bindings [a b b/c c.d c.d/e]
    {:a :aval :b :bval :b/c :bcval :c.d :cdval :c.d/e :cdeval}
    (is (= [a b c d e] [:aval :bval :bcval :cdval :cdeval]))))

(deftest links
  (with-query-bindings [[a _] [b 1] [c :x] [c/d _] [d.e _] [e.f/g _]]
    {:a :aval :b :bval :c :cval :c/d :cdval :d.e :deval :e.f/g :efgval}
    (is (= [a b c d e g] [:aval :bval :cval :cdval :deval :efgval]))))

(deftest joins-with-property-sources
  (with-query-bindings [{a [b c]} {d ...} {e 5}]
    {:a {:b :bval :c :cval} :d :dval :e :eval}
    (is (= [a b c d e] [{:b :bval :c :cval}
                        :bval :cval :dval :eval]))))

(deftest joins-with-link-sources
  (with-query-bindings [{[a _] [b c]} {[d _] ...}]
    {:a {:b :bval :c :cval} :d :dval}
    (is (= [a b c d] [{:b :bval :c :cval} :bval :cval :dval]))))

(deftest prefixed-properties
  (with-query-bindings [a [b c] d [e f] g.h [i]]
    {:a/b :abval :a/c :acval :d/e :deval :d/f :dfval :g.h/i :ghival}
    (is (= [b c e f i] [:abval :acval :deval :dfval :ghival]))))

(deftest aliased-regular-properties
  (with-query-bindings [a :as b b :as c]
    {:a :aval :b :bval}
    (is (= [b c] [:aval :bval]))))

(deftest aliased-links
  (with-query-bindings [[a _] :as b [b _] :as c]
    {:a :aval :b :bval}
    (is (= [b c] [:aval :bval]))))

(deftest aliased-joins
  (with-query-bindings [{a [b]} :as c {b [d]} :as e]
    {:a {:b :abval} :b {:d :bdval}}
    (is (= [c b e d] [{:b :abval} :abval
                      {:d :bdval} :bdval]))))

(deftest aliased-prefixed-properties
  (with-query-bindings [a [b :as c c :as d]]
    {:a/b :abval :a/c :acval}
    (is (= [c d] [:abval :acval]))))

(deftest parameterizations
  (with-query-bindings [(a {b c d e}) (b {f g})]
    {:a :aval :b :bval}
    (is (= [a b] [:aval :bval]))))

(deftest joins-with-sub-joins
  (with-query-bindings [{a [b [c]
                            d [e]
                            {f [g
                                {h [i]}]}]}]
    {:a {:b/c :abcval
         :d/e :adeval
         :f {:g :afgval
             :h {:i :afhival}}}}
    (is (= [a c e f g h i]
           [{:b/c :abcval
             :d/e :adeval
             :f {:g :afgval
                 :h {:i :afhival}}}
            :abcval
            :adeval
            {:g :afgval
             :h {:i :afhival}}
            :afgval
            {:i :afhival}
            :afhival]))))

(deftest joins-with-sub-links
  (with-query-bindings [{a [b [c] [d _]]}]
    {:a {:b/c :abcval :d :adval}}
    (is (= [a c d] [{:b/c :abcval :d :adval} :abcval :adval]))))

(deftest joins-with-sub-aliases
  (with-query-bindings [{[a _] [b [c :as d] d :as e]}]
    {:a {:b/c :abcval :d :adval}}
    (is (= [a d e] [{:b/c :abcval :d :adval} :abcval :adval]))))

(deftest deeply-nested-query-with-joins
  (with-query-bindings [{a [b {c [d :as e]}]}]
    {:a {:b :abval :c {:d :acdval}}}
    (is (= [a b c e]
           [{:b :abval :c {:d :acdval}}
            :abval
            {:d :acdval}
            :acdval]))))

(deftest backlink-joins
  (with-query-bindings [{a [_b [{c [d]}]]}]
    {:a {:b/_c {:d :abcdval}}}
    (is (= [a b d]
           [{:b/_c {:d :abcdval}} {:d :abcdval} :abcdval]))))

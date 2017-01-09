#!/usr/bin/env boot

(def +project+ 'workflo/macros)
(def +version+ "0.2.36")

(set-env!
 :resource-paths #{"resources" "src/main" "src/docs"}
 :dependencies '[;; Boot setup
                 [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                 [adzerk/boot-reload "0.4.13" :scope "test"]
                 [adzerk/boot-test "1.1.2" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [boot-codox "0.10.1" :scope "test" :scope "test"]
                 [pandeiro/boot-http "0.7.6" :scope "test"]
                 [crisptrutski/boot-cljs-test "0.2.2" :scope "test"]
                 [com.cemerick/piggieback "0.2.1"
                  :exclusions [com.google.guava/guava]
                  :scope "test"]

                 ;; Testing
                 [org.clojure/test.check "0.9.0" :scope "test"]

                 ;; Library dependencies
                 [bidi "2.0.14"]
                 [com.datomic/datomic-free "0.9.5407" :scope "test"
                  :exclusions [com.google.guava/guava]]
                 [com.stuartsierra/component "0.3.1"]
                 [datomic-schema "1.3.0"]
                 [inflections "0.12.2"]
                 [org.clojure/clojure "1.9.0-alpha11"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [org.clojure/data.json "0.2.6"]

                 ;; Development dependencies
                 [devcards "0.2.2" :scope "test"]
                 [datascript "0.15.4" :scope "test"]])


(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[adzerk.bootlaces :refer :all]
         '[boot.git :refer [last-commit]]
         '[codox.boot :refer [codox]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs exit!]]
         '[pandeiro.boot-http :refer [serve]])

(bootlaces! +version+ :dont-modify-paths? true)

(task-options!
 test-cljs {:js-env :phantom
            :update-fs? true
            :optimizations :none}
 push      {:repo "deploy-clojars"
            :ensure-branch "master"
            :ensure-clean true
            :ensure-tag (last-commit)
            :ensure-version +version+}
 pom       {:project +project+
            :version +version+
            :description "Clojure macros for web and mobile development"
            :url "https://github.com/workfloapp/macros"
            :scm {:url "https://github.com/workfloapp/macros"}
            :license {"MIT License"
                      "https://opensource.org/licenses/MIT"}}
 repl      {:middleware '[cemerick.piggieback/wrap-cljs-repl]})

(deftask examples
  []
  (merge-env! :source-paths #{"src/examples"})
  identity)

(deftask build-dev
  []
  (comp
    (cljs :source-map true
          :optimizations :none
          :compiler-options {:devcards true
                             :parallel-build true})))

(deftask build-production
  []
  (comp
   (cljs :optimizations :advanced
         :compiler-options {:devcards true
                            :parallel-build true})))

(deftask dev
  []
  (comp
    (examples)
    (watch)
    (reload :on-jsload 'workflo.macros.examples.screen-app/reload)
    (build-dev)
    (serve)
    (repl :server true)))

(deftask production
  []
  (comp
   (examples)
   (watch)
   (build-production)
   (serve)))

(deftask testing
  []
  (merge-env! :source-paths #{"src/test"})
  identity)

(deftask docs
  []
  (comp
   (codox :name "workflo/macros"
          :source-paths #{"src/main"}
          :output-path "api-docs"
          :metadata {:doc/format :markdown})
   (target)))

(deftask test-once
  []
  (comp
    (testing)
    (test-cljs)
    (test)
    (exit!)))

(deftask test-auto
  []
  (comp
    (testing)
    (watch)
    (test-cljs)
    (test)))

(deftask install-local
  []
  (comp
    (pom)
    (jar)
    (install)))

(deftask deploy-snapshot
  []
  (comp
    (pom)
    (jar)
    (push-snapshot)))

(deftask deploy-release
  []
  (comp
    (pom)
    (jar)
    (push-release)))

#!/usr/bin/env boot

(def +project+ 'workflo/macros)
(def +version+ "0.2.58")

(set-env!
 :resource-paths #{"resources" "src/main" "src/docs"}
 :dependencies '[;; Boot setup
                 [adzerk/boot-cljs "2.0.0" :scope "test"]
                 [adzerk/boot-reload "0.5.1" :scope "test"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [boot-codox "0.10.3" :scope "test"]
                 [pandeiro/boot-http "0.8.3" :scope "test"]
                 [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                 [com.cemerick/piggieback "0.2.2" :scope "test"
                  :exclusions [com.google.guava/guava]]

                 ;; Testing
                 [org.clojure/test.check "0.10.0-alpha2" :scope "test"]

                 ;; Library dependencies
                 [bidi "2.1.2"]
                 [com.datomic/datomic-free "0.9.5561.50" :scope "test"
                  :exclusions [com.google.guava/guava]]
                 [com.stuartsierra/component "0.3.2"]
                 [datomic-schema "1.3.0"]
                 [inflections "0.13.0"]
                 [org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/spec.alpha "0.1.123"]
                 [org.omcljs/om "1.0.0-alpha48"]
                 [org.clojure/data.json "0.2.6"]

                 ;; Development dependencies
                 [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                 [devcards "0.2.3" :scope "test"]
                 [datascript "0.16.1" :scope "test"]])


(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[adzerk.bootlaces :refer :all]
         '[boot.git :refer [last-commit]]
         '[codox.boot :refer [codox]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]]
         '[pandeiro.boot-http :refer [serve]])

(bootlaces! +version+ :dont-modify-paths? true)

(task-options!
 test-cljs {:js-env :phantom
            :update-fs? true
            :exit? true
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
    (test)))

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

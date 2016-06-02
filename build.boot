#!/usr/bin/env boot

(set-env!
 :resource-paths #{"resources" "src/main" "src/docs"}
 :dependencies '[;; Boot setup
                 [adzerk/boot-cljs "1.7.228-1"]
                 [adzerk/boot-reload "0.4.8"]
                 [adzerk/boot-test "1.1.1"]
                 [adzerk/bootlaces "0.1.13"]
                 [pandeiro/boot-http "0.7.3"]
                 [crisptrutski/boot-cljs-test "0.2.1"]

                 ;; Library dependencies
                 [org.clojure/clojurescript "1.9.14"]
                 [org.omcljs/om "1.0.0-alpha36"]
                 [org.clojure/data.json "0.2.6"]

                 ;; Development dependencies
                 [devcards "0.2.1-7"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[adzerk.bootlaces :refer :all]
         '[boot.git :refer [last-commit]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs exit!]]
         '[pandeiro.boot-http :refer [serve]])

(def version "0.2.5")

(bootlaces! version :dont-modify-paths? true)

(task-options!
 test-cljs {:js-env :phantom
            :update-fs? true
            :optimizations :none}
 push      {:repo "deploy-clojars"
            :ensure-branch "master"
            :ensure-clean true
            :ensure-tag (last-commit)
            :ensure-version version}
 pom       {:project 'workflo/macros
            :version version
            :description "Clojure macros for web and mobile development"
            :url "https://github.com/workfloapp/macros"
            :scm {:url "https://github.com/workfloapp/macros"}
            :license {"MIT License"
                      "https://opensource.org/licenses/MIT"}})

(deftask build-dev
  []
  (comp
    (cljs :source-map true
          :optimizations :none
          :compiler-options {:devcards true
                             :parallel-build true})))

(deftask build-docs
  []
  (comp
    (cljs :optimizations :advanced
          :compiler-options {:devcards true
                             :parallel-build true})))

(deftask dev
  []
  (comp
    (watch)
    (reload)
    (build-dev)
    (target)
    (serve :dir "target")))

(deftask docs
  []
  (comp
    (build-docs)))

(deftask testing
  []
  (merge-env! :source-paths #{"src/test"})
  identity)

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
    (build-jar)
    (target)
    (push-snapshot)))

(deftask deploy-release
  []
  (comp
    (pom)
    (jar)
    (build-jar)
    (target)
    (push-release)))

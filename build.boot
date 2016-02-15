#!/usr/bin/env boot

(set-env!
 :resource-paths #{"resources" "src/main" "src/docs"}
 :dependencies '[;; Boot setup
                 [adzerk/boot-cljs "1.7.228-1" :scope "test"]
                 [adzerk/boot-reload "0.4.5" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [pandeiro/boot-http "0.7.2" :scope "test"] 

                 ;; Library dependencies
                 [org.clojure/clojurescript "1.7.228"]
                 [org.omcljs/om "1.0.0-alpha30"]

                 ;; Development dependencies
                 [devcards "0.2.1-6"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-reload :refer [reload]]
         '[boot.git :refer [last-commit]]
         '[pandeiro.boot-http :refer [serve]])

(def version "0.1.0-alpha1-SNAPSHOT")

(bootlaces! version :dont-modify-paths? true)

(task-options!
  push {:repo "deploy"
        :ensure-branch "master"
        :ensure-clean true
        :ensure-tag (last-commit)
        :ensure-version version}
  pom {:project 'app-macros
       :version version
       :description "Clojure macros for web and mobile development"
       :url "https://github.com/workfloapp/app-macros"
       :scm {:url "https://github.com/workfloapp/app-macros"}
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

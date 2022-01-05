(defproject com.github.itarck/fancoil "0.0.7-SNAPSHOT"
  :description "A clojurescript framework, which uses multi-methods to define and implement system unit, uses integrant to inject configuration and stateful dependencies to unit at system startup."
  :url "https://github.com/itarck/fancoil"
  :license {:name "MIT License"
            :url  "https://opensource.org/licenses/MIT"}
  :source-paths ["src/main"]
  :dependencies [[org.clojure/core.async "1.5.648"]
                 [integrant/integrant "0.8.0"]
                 [reagent/reagent "1.1.0"]
                 [medley/medley "1.3.0"]])

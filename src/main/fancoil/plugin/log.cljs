(ns fancoil.plugin.log
  (:require 
   [fancoil.base :as base]))


(defmethod base/do! :log/out
  [_ _ value]
  (println "log/out: " value))


(defmethod base/do! :log/error
  [_ _ value]
  (println "log/error: " value))
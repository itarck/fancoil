(ns fancoil.lib.fx
  (:require
   [fancoil.base :as base]))


(defmethod base/do! :fx/doseq
  [env _ responses]
  (doseq [resp responses]
    (doseq [[k v] resp]
      (base/do! env k v))))
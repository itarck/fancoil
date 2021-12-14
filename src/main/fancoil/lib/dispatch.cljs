(ns fancoil.lib.dispatch
  (:require
   [fancoil.base :as base]))


(defmethod base/do! :dispatch/one
  [{:keys [dispatch]} _ request]
  (let [{signal :request/signal} request]
    (dispatch signal request)))

(defmethod base/do! :dispatch/many
  [{:keys [dispatch]} _ requests]
  (doseq [request requests]
    (let [{signal :request/signal} request]
      (dispatch signal request))))
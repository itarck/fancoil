(ns fancoil.lib.dispatch
  (:require
   [fancoil.base :as base]))


(defmethod base/do! :dispatch/request
  [{:keys [dispatch]} _ request]
  (let [{:request/keys [signal event]} request]
    (dispatch signal event)))

(defmethod base/do! :dispatch/requests
  [{:keys [dispatch]} _ requests]
  (doseq [request requests]
    (let [{:request/keys [signal event]} request]
      (dispatch signal event))))
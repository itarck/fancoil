(ns fancoil.plugin
  (:require
   [fancoil.base :as base]))

;; ratom

(defmethod base/inject :ratom/db
  [{:keys [ratom]} _fn req]
  (assoc req :ratom/db @ratom))

(defmethod base/do! :ratom/reset
  [{:keys [ratom]} _ value]
  (reset! ratom value))

;; dispatch 

(defmethod base/do! :dispatch/request
  [{:keys [dispatch]} _ request]
  (let [{:request/keys [method event]} request]
    (dispatch method event)))

(defmethod base/do! :dispatch/requests
  [{:keys [dispatch]} _ requests]
  (doseq [request requests]
    (let [{:request/keys [method event]} request]
      (dispatch method event))))

;; fx

(defmethod base/do! :do/effects
  [core _ responses]
  (doseq [resp responses]
    (doseq [[k v] resp]
      (base/do! core k v))))

(defmethod base/do! :do/effect
  [core _ response]
  (doseq [[k v] response]
    (base/do! core k v))
  response)

;; handle!

(defmethod base/handle! :default
  [{:keys [do! handle inject]} method req]
  (let [req (inject :ratom/db req)
        resp (handle method req)]
    (do! :do/effect resp)))


;; log

(defmethod base/do! :log/out
  [_ _ value]
  (println "log/out: " value))

(defmethod base/do! :log/error
  [_ _ value]
  (println "log/error: " value))


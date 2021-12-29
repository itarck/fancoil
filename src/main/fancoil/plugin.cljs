(ns fancoil.plugin
  (:require
   [medley.core :as m]
   [fancoil.base :as base]))

;; ratom

(defmethod base/inject :ratom/db
  [{:keys [ratom]} _fn req]
  (assoc req :ratom/db @ratom))

(defmethod base/do! :ratom/reset
  [{:keys [ratom]} _ db-value]
  (reset! ratom db-value))

(defmethod base/do! :ratom/set-paths
  [{:keys [ratom]} _ path-value-pairs]
  (doseq [[path value] path-value-pairs]
    (swap! ratom assoc-in path value)))

(defmethod base/do! :ratom/delete-paths
  [{:keys [ratom]} _ paths]
  (doseq [path paths]
    (swap! ratom m/dissoc-in path)))

;; dispatch 

(defmethod base/do! :dispatch/request
  [{:keys [dispatch]} _ request]
  (dispatch request))

(defmethod base/do! :dispatch/requests
  [{:keys [dispatch]} _ requests]
  (doseq [request requests]
    (dispatch request)))

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

;; process

(defmethod base/process :log/out
  [{:keys [do!]} _method request]
  (do! :log/out request))

(defmethod base/process :default
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

(ns fancoil.module.datascript.plugin
  (:require
   [datascript.core :as d]
   [fancoil.base :as base]))


(defmethod base/inject :ds/db
  [{:keys [conn]} _fn req]
  (assoc req :ds/db @conn))


(defmethod base/do! :ds/tx
  [{:keys [conn]} _ tx]
  (d/transact! conn tx)
  tx)


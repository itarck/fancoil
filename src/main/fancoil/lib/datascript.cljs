(ns fancoil.lib.datascript
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]
   [fancoil.base :as base]))


(defmethod ig/init-key :fancoil.lib/datascript [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    conn))


(defmethod base/inject :ds/db
  [{:keys [conn]} _fn req]
  (assoc req :ds/db @conn))


(defmethod base/do! :ds/tx
  [{:keys [conn]} _ tx]
  (d/transact! conn tx)
  tx)


(ns fancoil.modules.datascript
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]
   [fancoil.base :as base]))


(defmethod base/inject :inject.ds/db
  [{:keys [conn]} _fn req]
  (assoc-in req [:env :db] @conn))


(defmethod base/do! :do.ds/tx
  [{:keys [conn]} _ tx]
  (d/transact! conn tx)
  tx)


(defmethod ig/init-key :system/conn [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    conn))


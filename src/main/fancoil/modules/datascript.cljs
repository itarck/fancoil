(ns fancoil.modules.datascript
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]
   [fancoil.base :as b]))


(defmethod b/inject-base :ds-db
  [{:keys [conn]} _fn req]
  (assoc-in req [:env :db] @conn))


(defmethod b/do-base :ds-tx
  [{:keys [conn]} _ tx]
  (d/transact! conn tx)
  tx)


(defmethod ig/init-key :fancoil.units/conn [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    conn))


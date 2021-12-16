(ns fancoil.module.datascript.unit
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]))


(defmethod ig/init-key :fancoil.module.datascript/unit [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    conn))
(ns fancoil.module.posh.unit
  (:require
   [datascript.core :as d]
   [posh.reagent :as p]
   [integrant.core :as ig]
   [fancoil.module.posh.plugin]))

(defmethod ig/init-key :fancoil.module.posh/unit [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    (p/posh! conn)
    conn))
(ns fancoil.module.datascript.plugin
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]))


(defmethod ig/init-key ::datascript [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    conn))
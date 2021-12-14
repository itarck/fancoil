(ns fancoil.lib.posh
  (:require 
   [datascript.core :as d]
   [posh.reagent :as p]
   [integrant.core :as ig]))


(defmethod ig/init-key :fancoil.core/pconn [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    (p/posh! conn)
    conn))

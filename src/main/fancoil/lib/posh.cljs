(ns fancoil.lib.posh
  (:require 
   [datascript.core :as d]
   [posh.reagent :as p]
   [integrant.core :as ig]
   [fancoil.base :as base]))


(defmethod ig/init-key :fancoil.lib/posh [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    (p/posh! conn)
    conn))


(defmethod base/inject :posh/db
  [{:keys [pconn]} _fn req]
  (assoc req :posh/db @pconn))


(defmethod base/do! :posh/tx
  [{:keys [pconn]} _ tx]
  (p/transact! pconn tx)
  tx)


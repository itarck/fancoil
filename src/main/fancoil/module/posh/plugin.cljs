(ns fancoil.module.posh.plugin
  (:require
   [posh.reagent :as p]
   [fancoil.base :as base]))


(defmethod base/inject :posh/db
  [{:keys [pconn]} _fn req]
  (assoc req :posh/db @pconn))


(defmethod base/do! :posh/tx
  [{:keys [pconn]} _ tx]
  (p/transact! pconn tx)
  tx)


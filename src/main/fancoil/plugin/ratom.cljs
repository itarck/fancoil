(ns fancoil.plugin.ratom
  (:require
   [fancoil.base :as fcb]))


(defmethod fcb/inject :ratom/db
  [{:keys [ratom]} _fn req]
  (assoc req :ratom/db @ratom))


(defmethod fcb/do! :ratom/reset
  [{:keys [ratom]} _ value]
  (reset! ratom value))
(ns fancoil.lib.ratom
  (:require
   [integrant.core :as ig]
   [reagent.core :as r]
   [fancoil.base :as base]))


(defn create-ratom
  [config]
  (let [{:keys [initial-value]} config]
    (r/atom initial-value)))


(defmethod ig/init-key :fancoil.lib/ratom
  [_ config]
  (create-ratom config))

(defmethod base/inject :ratom/db
  [{:keys [ratom]} _fn req]
  (assoc req :ratom/db @ratom))


(defmethod base/do! :ratom/reset
  [{:keys [ratom]} _ value]
  (reset! ratom value))


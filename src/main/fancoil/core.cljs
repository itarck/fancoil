(ns fancoil.core
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [reagent.core :as r]
   [integrant.core :as ig]
   [fancoil.base :as base]

   [fancoil.lib.ratom :as ratom]
   [fancoil.lib.log]
   [fancoil.lib.fx]))


(defmethod ig/init-key ::ratom
  [_ config]
  (ratom/create-ratom config))


(defmethod ig/init-key ::tap
  [_ config]
  (partial base/tap config))

(defmethod ig/init-key ::inject
  [_ config]
  (partial base/inject config))

(defmethod ig/init-key ::do!
  [_ config]
  (partial base/do! config))

(defmethod ig/init-key ::doall!
  [_ {:keys [do!]}]
  (fn [resp]
    (doseq [[k v] resp]
      (do! k v))
    resp))

(defmethod ig/init-key ::handle
  [_ config]
  (partial base/handle config))

(defmethod ig/init-key ::handle!
  [_ config]
  (partial base/handle! config))

(defmethod ig/init-key ::subscribe
  [_ config]
  (partial base/subscribe config))

(defmethod ig/init-key ::view
  [_ config]
  (partial base/view config))

(defmethod ig/init-key ::chan
  [_ _]
  (chan))

(defmethod ig/init-key ::dispatch
  [_ {:keys [event-chan]}]
  (fn [signal event]
    (let [req {:request/signal signal
               :request/event event}]
      (go (>! event-chan req)))))

(defmethod ig/init-key ::service
  [_ {:keys [handle! event-chan]}]
  (go-loop []
    (let [request (<! event-chan)
          signal (:request/signal request)]
      (handle! signal request))
    (recur))
  {:event-chan event-chan})
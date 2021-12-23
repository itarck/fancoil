(ns fancoil.unit
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [reagent.core :as r]
   [integrant.core :as ig]
   [fancoil.base :as base]
   [fancoil.plugin]))


(defmethod ig/init-key ::ratom
  [_ config]
  (let [{:keys [initial-value]} config]
    (r/atom initial-value)))

(defmethod ig/init-key ::tap
  [_ config]
  (partial base/tap config))

(defmethod ig/init-key ::inject
  [_ config]
  (partial base/inject config))

(defmethod ig/init-key ::do!
  [_ config]
  (partial base/do! config))

(defmethod ig/init-key ::handle
  [_ config]
  (partial base/handle config))

(defmethod ig/init-key ::process
  [_ config]
  (partial base/process config))

(defmethod ig/init-key ::subscribe
  [_ config]
  (partial base/subscribe config))

(defmethod ig/init-key ::view
  [_ config]
  (partial base/view config))

(defmethod ig/init-key ::schedule
  [_ config]
  (partial base/schedule config))

(defmethod ig/init-key ::chan
  [_ _]
  (chan))

(defmethod ig/init-key ::dispatch
  [_ {:keys [event-chan]}]
  (fn dispatch
    ([method]
     (dispatch method {} {}))
    ([method event]
     (dispatch method event {:sync? false}))
    ([method event args]
     (let [req (-> {:request/method method
                    :request/event event}
                   ((fn [event]
                      (if (:sync? args)
                        (assoc event :request/sync? true)
                        event))))]
       (go (>! event-chan req))))))

(defmethod ig/init-key ::service
  [_ {:keys [process event-chan]}]
  (go-loop []
    (let [request (<! event-chan)
          {:request/keys [method sync?]} request]
      (if sync?
        (process method request)
        (go (process method request))))
    (recur))
  {:event-chan event-chan})




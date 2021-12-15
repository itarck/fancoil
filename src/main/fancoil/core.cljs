(ns fancoil.core
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [reagent.core :as r]
   [integrant.core :as ig]
   [fancoil.base :as base]

   [fancoil.lib.log]
   [fancoil.lib.dispatch]
   [fancoil.lib.fx]
   [fancoil.lib.ratom :as lib.ratom]
   [fancoil.lib.posh :as lib.posh]))


;; helper functions 

(defn load-hierarchy
  [hierarchy]
  (doseq [[tag parents] hierarchy
          parent parents]
    (derive tag parent)))

(defn merge-config
  [old-config new-config]
  (merge-with merge old-config new-config))


;; integrant units

(defmethod ig/init-key ::ratom
  [_ config]
  (lib.ratom/create-ratom config))

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
          {:request/keys [signal sync?]} request]
      (if sync?
        (handle! signal request)
        (go (handle! signal request))))
    (recur))
  {:event-chan event-chan})


;; default config

(def default-config
  {::ratom {}   
   ::tap {}
   ::inject {:ratom (ig/ref ::ratom)} 
   ::do! {:ratom (ig/ref ::ratom)}  
   ::doall! {:do! (ig/ref ::do!)}
   ::handle {:tap (ig/ref ::tap)}
   ::handle! {:ratom (ig/ref ::ratom)
              :handle (ig/ref ::handle)
              :inject (ig/ref ::inject)
              :do! (ig/ref ::do!)
              :doall! (ig/ref ::doall!)}
   ::subscribe {:ratom (ig/ref ::ratom)} 
   ::view {:dispatch (ig/ref ::dispatch)  
           :subscribe (ig/ref ::subscribe)}
   ::chan {}
   ::dispatch {:event-chan (ig/ref ::chan)}
   ::service {:handle! (ig/ref ::handle!)
              :event-chan (ig/ref ::chan)}})


(ns fancoil.core
  (:require
   [integrant.core :as ig]
   [fancoil.unit :as fu]
   [fancoil.plugin]))


;; helper functions 

(defn load-hierarchy
  [hierarchy]
  (doseq [[tag parents] hierarchy
          parent parents]
    (derive tag parent)))

(defn merge-config
  [old-config new-config]
  (merge-with merge old-config new-config))


(def default-config
  {::fu/ratom {}
   ::fu/tap {}
   ::fu/inject {:ratom (ig/ref ::fu/ratom)}
   ::fu/do! {:ratom (ig/ref ::fu/ratom)}
   ::fu/handle {:tap (ig/ref ::fu/tap)}
   ::fu/process {:ratom (ig/ref ::fu/ratom)
                 :handle (ig/ref ::fu/handle)
                 :inject (ig/ref ::fu/inject)
                 :do! (ig/ref ::fu/do!)}
   ::fu/subscribe {:ratom (ig/ref ::fu/ratom)}
   ::fu/view {:dispatch (ig/ref ::fu/dispatch)
              :subscribe (ig/ref ::fu/subscribe)}
   ::fu/chan {}
   ::fu/dispatch {:event-chan (ig/ref ::fu/chan)}
   ::fu/service {:process (ig/ref ::fu/process)
                 :event-chan (ig/ref ::fu/chan)}
   ::fu/schedule {:dispatch (ig/ref ::fu/dispatch)}})


(ns input-demo
  (:require
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.system :as fs]))


(defmethod fb/view-base :app/view
  [{:keys [ratom]} _ _]
  (let [value @(r/cursor ratom [:value])]
    [:div
     [:input {:value value
              :on-change (fn [e]
                           (let [new-value (j/get-in e [:target :value])]
                             (swap! ratom assoc :value new-value)))}]
     [:p "current value: " value]]))


;; -------------------------
;; integrant 

(def config
  {::fs/ratom {:initial-value {}}
   ::fs/view {:ratom (ig/ref ::fs/ratom)}})

(defonce system-core
  (ig/init config))

(defonce system-instance
  (fs/create-instance fs/system-base system-core))

;; -------------------------
;; Initialize app

(defn mount-root
  []
  (let [v (system-instance ::fs/view)]
    (rdom/render [v :app/view {}]
                 (js/document.getElementById "app"))))


(defn ^:export init! []
  (mount-root))


(comment

  (system-instance ::fs/ratom)
  (system-instance :keys)

  )
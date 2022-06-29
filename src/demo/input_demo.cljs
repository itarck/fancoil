(ns input-demo
  (:require
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
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
  {::fu/ratom {:initial-value {}}
   ::fu/view {:ratom (ig/ref ::fu/ratom)}})

(defonce system-core
  (ig/init config))

(defonce system-instance
  (fs/create-instance fs/system-base system-core))

;; -------------------------
;; Initialize app

(defn mount-root
  []
  (let [v (system-instance ::fu/view)]
    (rdom/render [v :app/view {}]
                 (js/document.getElementById "app"))))


(defn ^:export init! []
  (mount-root))


(comment

  (system-instance ::fu/ratom)
  (system-instance :keys)

  )
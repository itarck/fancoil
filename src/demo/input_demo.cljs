(ns input-demo
  (:require
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.system :as fs]))

;; -----------------------------------------
;; handle 

(defmethod fb/handle-base :app/set-value
  [_ _ {:keys [new-value]}]
  {:set-ratom-paths {[:value] new-value}})

;; -----------------------------------------
;; subs

(defmethod fb/subscribe-base :app/get-value
  [{:keys [ratom]} _ _]
  (r/cursor ratom [:value]))


(defmethod fb/view-base :app/view
  [{:keys [dispatch subscribe]} _ props]
  (let [value @(subscribe :app/get-value {})]
    [:div
     [:input {:value value
              :on-change (fn [e]
                           (let [new-value (j/get-in e [:target :value])]
                             (dispatch :app/set-value {:new-value new-value})))}]
     [:p "current value: " value]]))


;; -------------------------
;; integrant 

;; you can write it from scratch

(def config
  {::fs/ratom {:initial-value {}}
   ::fs/inject {:ratom (ig/ref ::fs/ratom)
                :inject-keys [:ratom-db]}
   ::fs/do! {:ratom (ig/ref ::fs/ratom)}
   ::fs/handle {}
   ::fs/process {:ratom (ig/ref ::fs/ratom)
                 :handle (ig/ref ::fs/handle)
                 :inject (ig/ref ::fs/inject)
                 :do! (ig/ref ::fs/do!)}
   ::fs/subscribe {:ratom (ig/ref ::fs/ratom)}
   ::fs/view {:dispatch (ig/ref ::fs/dispatch)
              :subscribe (ig/ref ::fs/subscribe)}
   ::fs/chan {}
   ::fs/dispatch {:out-chan (ig/ref ::fs/chan)}
   ::fs/service {:process (ig/ref ::fs/process)
                 :in-chan (ig/ref ::fs/chan)}})


(defonce system
  (ig/init config))


;; -------------------------
;; Initialize app

(defn mount-root
  []
  (rdom/render [(::fs/view system) :app/view {}]
               (js/document.getElementById "app")))


(defn ^:export init! []
  (mount-root))


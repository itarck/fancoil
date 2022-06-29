(ns clock-demo
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]))


;; -----------------------------------------
;; handle 


(defmethod fb/handle-base :app/initialize
  [_ _ _]
  (let [new-db {:time (js/Date.)
                :time-color "#f88"}]
    {:reset-ratom new-db}))

(defmethod fb/handle-base :clock/time-color-change
  [_ _ {:keys [new-color-value]}]
  {:set-ratom-paths {[:time-color] new-color-value}})

(defmethod fb/handle-base :clock/timer
  [_ _ {:keys [new-time]}]
  {:set-ratom-paths {[:time] new-time}
   :log-out new-time})

;; -----------------------------------------
;; subs

(defmethod fb/subscribe-base :clock/time
  [{:keys [ratom]} _ _]
  (r/cursor ratom [:time]))

(defmethod fb/subscribe-base :clock/time-string
  [core _ _]
  (let [time @(fb/subscribe-base core :clock/time {})]
    (r/reaction (if time
                  (-> (.toTimeString time)
                      (str/split " ")
                      first)
                  ""))))

(defmethod fb/subscribe-base :clock/time-color
  [{:keys [ratom]} _ _]
  (r/cursor ratom [:time-color]))


;; -----------------------------------------
;; views

(defmethod fb/view-base :clock/timer
  [{:keys [subscribe]} _ _input]
  (let [time-string @(subscribe :clock/time-string {})
        color @(subscribe :clock/time-color {})]
    [:div.example-clock
     {:style {:color color}}
     time-string]))


(defmethod fb/view-base :clock/color-input
  [{:keys [dispatch subscribe]} _ _input]
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @(subscribe :clock/time-color {})
            :on-change #(dispatch :clock/time-color-change {:new-color-value (-> % .-target .-value)})}]])


(defmethod fb/view-base :app/root-view
  [core _ props]
  [:div
   [:h1 "Hello world, it is now"]
   [fb/view-base core :clock/timer {}]
   [fb/view-base core :clock/color-input {}]])


;; -----------------------------------------
;; schedule


(defmethod fb/schedule-base :clock/start-tictac
  [{:keys [dispatch]} _ {:keys [interval]}]
  (let [tictac (fn [] (dispatch :clock/timer {:new-time (js/Date.)}))]
    (js/setInterval tictac interval)))


;; -------------------------
;; integrant 

;; you can write it from scratch

(def config
  {::fu/ratom {:initial-value {}}
   ::fu/inject {:ratom (ig/ref ::fu/ratom)
                :inject-keys [:ratom-db]}
   ::fu/do! {:ratom (ig/ref ::fu/ratom)}
   ::fu/handle {}
   ::fu/process {:ratom (ig/ref ::fu/ratom)
                 :handle (ig/ref ::fu/handle)
                 :inject (ig/ref ::fu/inject)
                 :do! (ig/ref ::fu/do!)}
   ::fu/subscribe {:ratom (ig/ref ::fu/ratom)}
   ::fu/view {:dispatch (ig/ref ::fu/dispatch)
              :subscribe (ig/ref ::fu/subscribe)
              :schedule (ig/ref ::fu/schedule)}
   ::fu/chan {}
   ::fu/dispatch {:out-chan (ig/ref ::fu/chan)}
   ::fu/schedule {:dispatch (ig/ref ::fu/dispatch)}
   ::fu/service {:process (ig/ref ::fu/process)
                 :in-chan (ig/ref ::fu/chan)}})


(defonce system
  (ig/init config))


;; -------------------------
;; Initialize app


(defn mount-root
  []
  (let [dispatch (::fu/dispatch system)
        schedule (::fu/schedule system)]
    (dispatch :app/initialize {:_props {:sync? true}})
    (schedule :clock/start-tictac {:interval 1000}))

  (rdom/render [(::fu/view system) :app/root-view {}]
               (js/document.getElementById "app")))


(defn ^:export init! []
  (mount-root))


(comment 
  
  system
  )
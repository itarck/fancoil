(ns mounter-demo
  (:require
   [applied-science.js-interop :as j]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]
   [fancoil.modules.mounter]))

;; pager 

(defmethod fb/pager-base :app/input-page
  [_ _]
  {:on-load [:app.input-page/on-load]})

;; view

(defmethod fb/view-base :app/home-page
  [{:keys [pager]} _ props]
  [:div
   "this is home-page"
   [:div (str props)]
   [:div
    [:div [:button {:on-click #(pager :change-page {:page [:app/item-page {:id 1}]})}
           "navigate to item1"]]
    [:div [:button {:on-click #(pager :change-page {:page [:app/item-page {:id 2}]})}
           "navigate to item2"]]
    [:div [:button {:on-click #(pager :change-page {:page [:app/input-page ]})}
           "navigate to input page"]]]])


(defmethod fb/view-base :app/item-page
  [{:keys [pager cache]} _ props]
  [:div
   "this is item-page" (str props)
   [:p (str @cache)]
   [:div 
    [:button {:on-click #(pager :change-page {:page [:app/home-page]})} "home page"]]])

(defmethod fb/view-base :app/input-page
  [{:keys [pager cache ratom]} _ _]
  (let [value (:value @cache)]
    [:div
     [:p (str @cache)]
     [:input {:value value
              :on-change (fn [e]
                           (let [new-value (j/get-in e [:target :value])]
                             (swap! cache assoc :value new-value)
                             (swap! ratom assoc :value new-value)))}]
     [:p "current value: " value]
     [:div
      [:button {:on-click #(pager :change-page {:page [:app/home-page]})} "home page"]]]
    ))


;; handle 

(defmethod fb/handle-base :app.input-page/on-load
  [{:keys [ratom cache]} _]
  (swap! cache assoc :value (:value @ratom))
  )

;; integrant


(def hierarchy
  {::pconn [::fu/pconn]
   ::ratom [::fu/ratom]
   ::cache [::fu/ratom]
   ::schema [::fu/schema]
   ::inject [::fu/inject]
   ::do! [::fu/do!]
   ::model [::fu/model]
   ::handle [::fu/handle]
   ::process [::fu/process]
   ::subscribe [::fu/subscribe]
   ::view [::fu/view]
   ::chan [::fu/chan]
   ::dispatch [::fu/dispatch]
   ::service [::fu/service]
   ::mounter [::fu/mounter]
   ::pager [::fu/pager]})


(fs/load-hierarchy hierarchy)

(def config
  {::schema {}
   ::ratom {}
   ::pconn {:schema (ig/ref ::schema)
            :initial-tx []}
   ::cache {}
   ::inject {:pconn (ig/ref ::pconn)}
   ::do! {:dispatch (ig/ref ::dispatch)
          :subscribe (ig/ref ::subscribe)
          :pconn (ig/ref ::pconn)}
   ::model {}
   ::handle {:subscribe (ig/ref ::subscribe)
             :model (ig/ref ::model)
             :ratom (ig/ref ::ratom)
             :cache (ig/ref ::cache)}
   ::process {:handle (ig/ref ::handle)
              :inject (ig/ref ::inject)
              :do! (ig/ref ::do!)}
   ::subscribe {:pconn (ig/ref ::pconn)}
   ::view {:dispatch (ig/ref ::dispatch)
           :subscribe (ig/ref ::subscribe)
           :cache (ig/ref ::cache)
           :ratom (ig/ref ::ratom)
           :pager (ig/ref ::pager)}
   ::chan {}
   ::dispatch {:out-chan (ig/ref ::chan)}
   ::service {:process (ig/ref ::process)
              :in-chan (ig/ref ::chan)}
   ::pager {:initial-page [:app/home-page]
            :cache (ig/ref ::cache)
            :dispatch (ig/ref ::dispatch)}
   ::mounter {:pager (ig/ref ::pager)
              :dom-id "app"
              :view (ig/ref ::view)}
    })


(defonce sys
  (let [core (ig/init config)]
    (fs/create-instance fs/system-base core)))

(defonce subscribe
  (sys ::subscribe))

(defonce handle
  (sys ::handle))

(defonce process
  (sys ::process))

;; -------------------------
;; Initialize app


(defn mount-root []
  (sys ::mounter :mount!))

(defn dev-init!
  [])

(defn init! []
  (dev-init!)
  (mount-root))



(comment 
  (sys ::pager :get-state)
  )
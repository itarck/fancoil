(ns mounter-demo
  (:require
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]
   [fancoil.modules.mounter]))

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
           "navigate to item2"]]]])


(defmethod fb/view-base :app/item-page
  [{:keys [pager]} _ props]
  [:div
   "this is item-page" (str props)
   [:div 
    [:button {:on-click #(pager :change-page {:page [:app/home-page]})} "home page"]]])


;; integrant


(def hierarchy
  {::pconn [::fu/pconn]
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
   ::pconn {:schema (ig/ref ::schema)
            :initial-tx []}
   ::inject {:pconn (ig/ref ::pconn)}
   ::do! {:dispatch (ig/ref ::dispatch)
          :subscribe (ig/ref ::subscribe)
          :pconn (ig/ref ::pconn)}
   ::model {}
   ::handle {:subscribe (ig/ref ::subscribe)
             :model (ig/ref ::model)}
   ::process {:handle (ig/ref ::handle)
              :inject (ig/ref ::inject)
              :do! (ig/ref ::do!)}
   ::subscribe {:pconn (ig/ref ::pconn)}
   ::view {:dispatch (ig/ref ::dispatch)
           :subscribe (ig/ref ::subscribe)
           :pager (ig/ref ::pager)}
   ::chan {}
   ::dispatch {:out-chan (ig/ref ::chan)}
   ::service {:process (ig/ref ::process)
              :in-chan (ig/ref ::chan)}
   ::pager {:initial-page [:app/home-page]}
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




(ns router-demo
  (:require
   [cljs.pprint :refer [pprint]]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]
   [fancoil.modules.cljs-ajax]
   [fancoil.modules.router]
   [reitit.coercion.spec]))

;; view

(defmethod fb/view-base :app/home-page
  [{:keys [router]} _ props]
  [:div
   "this is home-page"
   [:div (str props)]
   [:div
    [:a {:href (router :path-for [:app/item-page {:id 1}])}
     "item 1"]]
   [:div
    [:button {:on-click #(router :navigate [:app/item-page {:id 1}])}
     "navigate to item1"]]])

(defmethod fb/view-base :app/item-page
  [_ _ props]
  [:div
   "this is item-page"
   [:div (str props)]])

(defmethod fb/view-base :app/root-page
  [{:keys [router] :as core} _ _]
  (let [current-route (:current-route @(router :router-atom))
        {:keys [page-name params]} current-route]
    [fb/view-base core page-name (assoc params :_env current-route)]))


;; integrant

(def routes
  [["/" {:name :app/home-page}]
   ["/item/:id" {:name :app/item-page
                 :coercion reitit.coercion.spec/coercion
                 :parameters {:path {:id int?}}}]])

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
   ::router [::fu/router]})

(fs/load-hierarchy hierarchy)

(def config
  {::schema {}
   ::pconn {:schema (ig/ref ::schema)
            :initial-tx []}
   ::inject {:pconn (ig/ref ::pconn)
             :router (ig/ref ::router)
             :inject-keys [:current-route]}
   ::do! {:dispatch (ig/ref ::dispatch)
          :subscribe (ig/ref ::subscribe)
          :pconn (ig/ref ::pconn)
          :router (ig/ref ::router)}
   ::model {}
   ::handle {:subscribe (ig/ref ::subscribe)
             :model (ig/ref ::model)}
   ::process {:handle (ig/ref ::handle)
              :inject (ig/ref ::inject)
              :do! (ig/ref ::do!)}
   ::subscribe {:pconn (ig/ref ::pconn)}
   ::view {:dispatch (ig/ref ::dispatch)
           :subscribe (ig/ref ::subscribe)
           :router (ig/ref ::router)}
   ::chan {}
   ::dispatch {:out-chan (ig/ref ::chan)}
   ::service {:process (ig/ref ::process)
              :in-chan (ig/ref ::chan)}
   ::router {:routes routes
                  :dispatch (ig/ref ::dispatch)
                  :on-navigate-hook :app/on-router-navigate}})


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
  (rdom/render
   [(sys ::view) :router-page {}]
   (.getElementById js/document "app")))

(defn dev-init!
  [])

(defn init! []
  (dev-init!)
  (mount-root))


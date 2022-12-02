(ns mount-demo
  (:require
   [applied-science.js-interop :as j]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]
   [fancoil.modules.mount]))

;; page 

(defmethod fb/page-base :app/input-page
  [{:keys [ratom cache]} _]
  {:on-load-hook #(swap! cache assoc :value (:value @ratom))})

;; view

(defmethod fb/view-base :app/home-page
  [{:keys [nav]} _ props]
  [:div
   "this is home-page"
   [:div (str props)]
   [:div
    [:div [:button {:on-click #(nav :app/item-page {:id 1})}
           "navigate to item1"]]
    [:div [:button {:on-click #(nav :app/item-page {:id 2})}
           "navigate to item2"]]
    [:div [:button {:on-click #(nav :app/input-page {})}
           "navigate to input page"]]]])


(defmethod fb/view-base :app/item-page
  [{:keys [nav cache]} _ props]
  [:div
   "this is item-page" (str props)
   [:p (str @cache)]
   [:div
    [:button {:on-click #(nav :app/home-page)} "home page"]]])


(defmethod fb/view-base :app/input-page
  [{:keys [nav cache ratom]} _ _]
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
      [:button {:on-click #(nav :app/home-page)} "home page"]]]))

;; integrant


(def hierarchy
  {::ratom [::fu/ratom]
   ::cache [::fu/ratom]
   ::schema [::fu/schema]
   ::do! [::fu/do!]
   ::model [::fu/model]
   ::handle [::fu/handle]
   ::process [::fu/process]
   ::view [::fu/view]
   ::chan [::fu/chan]
   ::dispatch [::fu/dispatch]
   ::service [::fu/service]
   ::mount [::fu/mount]
   ::page [::fu/page]
   ::nav [::fu/nav]})


(fs/load-hierarchy hierarchy)

(def config
  {::schema {}
   ::ratom {}
   ::cache {}
   ::do! {:dispatch (ig/ref ::dispatch)
          :page (ig/ref ::page)}
   ::model {}
   ::handle {:model (ig/ref ::model)
             :ratom (ig/ref ::ratom)
             :cache (ig/ref ::cache)}
   ::process {:handle (ig/ref ::handle)
              :do! (ig/ref ::do!)}
   ::view {:dispatch (ig/ref ::dispatch)
           :page (ig/ref ::page)
           :nav (ig/ref ::nav)
           :cache (ig/ref ::cache)
           :ratom (ig/ref ::ratom)}
   ::chan {}
   ::dispatch {:out-chan (ig/ref ::chan)}
   ::service {:process (ig/ref ::process)
              :in-chan (ig/ref ::chan)}
   ::page {:initial-page [:app/home-page]
            :cache (ig/ref ::cache)
            :ratom (ig/ref ::ratom)
            :dispatch (ig/ref ::dispatch)}
   ::nav {:page (ig/ref ::page)}
   ::mount {:page (ig/ref ::page)
              :dom-id "app"
              :view (ig/ref ::view)}})


(defonce sys
  (let [core (ig/init config)]
    (fs/create-instance fs/system-base core)))

(defonce handle
  (sys ::handle))

(defonce process
  (sys ::process))

;; -------------------------
;; Initialize app


(defn mount-root []
  (sys ::mount :mount!))

(defn dev-init!
  [])

(defn init! []
  (dev-init!)
  (mount-root))



(comment
  (sys ::page :get-state))
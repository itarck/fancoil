(ns ds-demo
  (:require
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [integrant.core :as ig]
   [fancoil.base :as fb]
   [fancoil.units :as fu]
   [fancoil.system :as fs]))


(defmethod fb/view-base :app/view
  [{:keys [subscribe dispatch]} _ ]
  (let [app @(subscribe :pull {:id [:app/name "unique"]})]
    [:div
     [:p "app value: " (str (:app/value app))]
     [:input {:value "Add"
              :type :button
              :on-click (fn [e]
                          (dispatch :app/add-value {:id (:db/id app)}))}]]))

(defmethod fb/handle-base :app/add-value
  [{:keys [read]} _ {:keys [id]}]
  (let [app (read :pull {:id id})
        new-value (inc (:app/value app))]
    {:tx [[:db/add id :app/value new-value]]}))

;; -------------------------
;; integrant 


(def config
  {::fu/pconn {:schema {:app/name {:db/unique :db.unique/identity}}
               :initial-tx [{:app/name "unique"
                             :app/value 35}]}
   ::fu/model {}
   ::fu/read {:pconn (ig/ref ::fu/pconn)}
   ::fu/subscribe {:pconn (ig/ref ::fu/pconn)}
   ::fu/handle {:model (ig/ref ::fu/model)
                :read (ig/ref ::fu/read)}
   ::fu/do! {:dispatch (ig/ref ::fu/dispatch)
             :pconn (ig/ref ::fu/pconn)}
   ::fu/process {:handle (ig/ref ::fu/handle)
                 :do! (ig/ref ::fu/do!)}
   ::fu/view {:dispatch (ig/ref ::fu/dispatch)
              :subscribe (ig/ref ::fu/subscribe)}
   ::fu/chan {}
   ::fu/dispatch {:out-chan (ig/ref ::fu/chan)}
   ::fu/service {:process (ig/ref ::fu/process)
                 :in-chan (ig/ref ::fu/chan)}})


(defonce sys
  (let [core (ig/init config)]
    (fs/create-instance fs/system-base core)))

;; -------------------------
;; Initialize app

(defn mount-root
  []
  (let [v (sys ::fu/view)]
    (rdom/render [v :app/view {}]
                 (js/document.getElementById "app"))))


(defn ^:export init! []
  (mount-root))


(comment

  (sys :keys)

  (sys ::fu/pconn)
  ;; 
  )
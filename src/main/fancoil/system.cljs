(ns fancoil.system
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [integrant.core :as ig]
   [reagent.dom :as rdom]
   [reagent.core :as r]

   [fancoil.units :as units]))

;; helper function

(defmethod ig/init-key ::value
  [_ config]
  config)

(defmethod ig/init-key ::spec
  [_ config]
  (units/create-spec-instance config))

(defmethod ig/init-key ::ratom
  [_ config]
  (units/create-ratom config))

(defmethod ig/init-key ::pconn
  [_k config]
  (units/create-poshed-datascript-conn config))

(defmethod ig/init-key ::schema
  [_ config]
  (units/create-schema-instance config))

(defmethod ig/init-key ::subscribe
  [_ config]
  (units/create-subscribe-instance config))

(defmethod ig/init-key ::view
  [_ config]
  (units/create-view-instance config))

(defmethod ig/init-key ::model
  [_ config]
  (units/create-model-instance config))

(defmethod ig/init-key ::handle
  [_ config]
  (units/create-handle-instance config))

(defmethod ig/init-key ::schedule
  [_ config]
  (units/create-schedule-instance config))

(defmethod ig/init-key ::chan
  [_ _]
  (chan))

(defmethod ig/init-key ::dispatch
  [_ config]
  (units/create-dispatch-instance config))

(defmethod ig/init-key ::inject
  [_ config]
  (units/create-inject-instance config))

(defmethod ig/init-key ::do!
  [_ config]
  (units/create-do-instance config))

(defmethod ig/init-key ::process
  [_ config]
  (units/create-process-instance config))

(defmethod ig/init-key ::service
  [_ config]
  (units/create-service-instance config))

;; ------------------------------------------------
;; helper functions
  
(defn load-hierarchy
  [hierarchy]
  (doseq [[tag parents] hierarchy
          parent parents]
    (derive tag parent)))

(defn merge-config
  [old-config new-config]
  (merge-with merge old-config new-config))


;; ------------------------------------------------
;; system

(defmulti system-base
  (fn [config method & args] method))

(defmethod system-base :reload
  [{:keys [state-atom config]} _ user-config]
  (let [instance (ig/init (merge-config config user-config))]
    (reset! state-atom instance)))

(defmethod system-base :get-state
  [core _ _]
  (let [instance @(:state-atom core)]
    instance))

(defmethod system-base :get-unit
  [{:keys [info] :as core} _ unit-key]
  (let [instance (system-base core :get-state {})]
    (if (qualified-keyword? unit-key)
      (unit-key instance)
      ((unit-key info) instance))))

(defmethod system-base :mount-view
  [{:keys [info] :as core} _ view-method view-ctx]
  (let [viewer (system-base core :get-unit :view)
        mount-element-id (or (:mount-element-id info) "app")]
    (rdom/render
     [viewer view-method view-ctx]
     (.getElementById js/document mount-element-id))))

(defmethod system-base :view
  [core _ method request]
  (let [view-unit (system-base core :get-unit :view)]
    [view-unit method request]))

(defmethod system-base :dispatch
  [core _ method request]
  (let [dispatch-unit (system-base core :get-unit :dispatch)]
    (dispatch-unit method request)))

(defmethod system-base :subscribe
  [core _ method request]
  (let [subscribe-unit (system-base core :get-unit :subscribe)]
    (subscribe-unit method request)))

(defmethod system-base :do!
  [core _ method request]
  (let [do-unit (system-base core :get-unit :do!)]
    (do-unit method request)))

(derive ::info ::value)

(def default-config
  {::info {:view ::view
           :dispatch ::dispatch
           :subscribe ::subscribe}
   ::schema {}
   ::spec {}
   ::pconn {:schema (ig/ref ::schema)
            :initial-tx []}
   ::subscribe {:pconn (ig/ref ::pconn)}
   ::inject {:pconn (ig/ref ::pconn)
             :inject-keys [:posh-db]}
   ::do! {:pconn (ig/ref ::pconn)
          :dispatch (ig/ref ::dispatch)}
   ::model {}
   ::handle {:schema (ig/ref ::schema)
             :model (ig/ref ::model)}
   ::process {:inject (ig/ref ::inject)
              :do! (ig/ref ::do!)
              :handle (ig/ref ::handle)}
   ::view {:schema (ig/ref ::schema)
           :dispatch (ig/ref ::dispatch)
           :subscribe (ig/ref ::subscribe)}
   ::chan {}
   ::dispatch {:out-chan (ig/ref ::chan)}
   ::service {:process (ig/ref ::process)
              :in-chan (ig/ref ::chan)}})

(defn create-system-instance
  [config]
  (let [core {:state-atom (r/atom (ig/init config))
              :config config
              :info (::info config)}]
    (fn [method & args]
      (apply system-base core method args))))


(comment 
  (ig/init default-config)
  
  )
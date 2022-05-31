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
  (fn [core method & args] method))

(defmethod system-base :core
  [core _]
  core)

(defmethod system-base :keys
  [core _]
  (keys core))

(defmethod system-base :methods
  [core _]
  (keys (methods system-base)))

(defmethod system-base :default
  [core method & args]
  (if (contains? core method)
    (if (seq args)
      (apply (get core method) args)
      (get core method))
    (throw (ex-info "system-base method not match"
                    {:method method
                     :args args}))))

(derive ::info ::value)

(def default-config
  {::info {:project-name "default"}
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

(defn create-instance
  [base-fn core]
  (fn instance [method & args]
    (apply base-fn core method args)))

(comment 
  (ig/init default-config)
  
  )
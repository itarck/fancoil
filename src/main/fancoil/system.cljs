(ns fancoil.system
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [integrant.core :as ig]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [fancoil.base :as b]
   [fancoil.units :as u]))


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

(defn create-instance
  [base-fn core]
  (fn instance [method & args]
    (apply base-fn core method args)))

(defn reg
  [fn-type fn-method function]
  (cond
    (= fn-type :subscribe) (defmethod b/subscribe-base fn-method
                             [core _ & args]
                             (apply function core args))
    (= fn-type :view) (defmethod b/view-base fn-method
                        [core _ & args]
                        (apply function core args))
    (= fn-type :handle) (defmethod b/handle-base fn-method
                          [core _ & args]
                          (apply function core args))
    (= fn-type :component) (defmethod b/component-base fn-method
                          [core _ & args]
                          (apply function core args))
    :else (throw (js/Error. "function type error"))))

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

(def hierarchy
  {::info [::u/value]
   ::schema [::u/schema]
   ::pconn [::u/pconn]
   ::inject [::u/inject]
   ::do! [::u/do!]
   ::model [::u/model]
   ::handle [::u/handle]
   ::process [::u/process]
   ::subscribe [::u/subscribe]
   ::component [::u/component]
   ::view [::u/view]
   ::chan [::u/chan]
   ::dispatch [::u/dispatch]
   ::service [::u/service]
   ::router [::u/router]})

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



(ns fancoil.modules.easy-router
  (:require
   [fancoil.base :as b]
   [integrant.core :as ig]
   [reagent.core :as r]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]))

;; base

(defmulti router-base
  (fn [core method & args] method))

(defmethod router-base :navigate
  [core _ route]
  (apply rfe/push-state route))

(defmethod router-base :path-for
  [core _ page-args]
  (apply rfe/href page-args))

(defmethod router-base :router-atom
  [core _ _]
  (:router-atom core))

(defmethod router-base :current-route
  [{:keys [router-atom]} _ ]
  @router-atom)

(defmethod router-base :with-path
  [core _ cursor]
  (let [current-route (router-base core :current-route)]
    (into [(:path current-route)] cursor)))


;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [router]} _ request]
  (let [current-route @(router :router-atom)]
    (update-in request [:_env] (fn [env] (merge env current-route)))))

(defmethod b/do-base :navigate
  [{:keys [router]} _ path-or-data]
  (router :navigate path-or-data))

(defmethod b/view-base :router-page
  [{:keys [router] :as core} _ _]
  (let [match @(router :router-atom)
        path-params (or (get-in match [:parameters :path])
                        (:path-params match))
        query-params (or (get-in match [:parameters :query])
                         (:query-params match))
        params (merge path-params query-params)
        page-name (:name (:data match))
        current-route {:page-name page-name
                       :params (merge path-params query-params)
                       :query-params query-params
                       :path-params path-params}]
    [b/view-base core name (assoc params :_env current-route)]))

;; -------------------------
;; Routes

(defn create-router-instance
  [config]
  (let [{:keys [routes dispatch before-navigate-method after-navigate-method]} config
        core {:router-atom (r/atom nil)}]
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion}})
     (fn [m]
       (let [prev-route @(:router-atom core)]
         (when before-navigate-method
           (dispatch before-navigate-method {:prev-route prev-route
                                             :current-route m}))
         (reset! (:router-atom core) m)
         (when after-navigate-method
           (dispatch after-navigate-method {:prev-route prev-route
                                            :current-route m}))))
     
    ;; set to false to enable HistoryAPI
     {:use-fragment true})
    (partial router-base core)))

(defmethod ig/init-key :fancoil.units/easy-router
  [_ config] 
  (create-router-instance config))

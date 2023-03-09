(ns fancoil.modules.easy-router
  (:require
   [fancoil.base :as b]
   [integrant.core :as ig]
   [reagent.core :as r]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]))

;; base

(defn parse-match
  [match]
  (let [path-params (or (get-in match [:parameters :path])
                        (:path-params match))
        query-params (or (get-in match [:parameters :query])
                         (:query-params match))
        params (merge path-params query-params)
        page-name (:name (:data match))
        current-route {:name page-name
                       :query-params query-params
                       :path-params path-params}]
    (assoc params
           :_route current-route
           :_data (:data match))))

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

(defmethod router-base :root-page
  [{:keys [router-atom view] :as core} _]
  (let [match @router-atom
        props (parse-match match)
        page-name (:name (:data match))]
    (if page-name
      [view page-name props]
      [:div
       "Page not found"])))

;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [router]} _ request]
  (let [current-route @(router :router-atom)]
    (update-in request [:_env] (fn [env] (merge env current-route)))))

(defmethod b/do-base :navigate
  [{:keys [router]} _ path-or-data]
  (router :navigate path-or-data))


;; -------------------------
;; Routes

(defn create-router-instance
  [config]
  (let [{:keys [routes dispatch]} config
        core (assoc config :router-atom (r/atom nil))]
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion}})
     (fn [match]
       (let [on-load (get-in match [:data :on-load])]
         (when on-load
           (dispatch on-load (parse-match match))))
       (reset! (:router-atom core) match))
     
    ;; set to false to enable HistoryAPI
     {:use-fragment true})
    (partial router-base core)))

(defmethod ig/init-key :fancoil.units/easy-router
  [_ config] 
  (create-router-instance config))


(ns fancoil.modules.router
  (:require
   [reagent.core :as r]
   [integrant.core :as ig]
   [reitit.frontend :as rfront]
   [reitit.coercion :as coercion]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [fancoil.base :as b]))

;; base

(defmulti router-base
  (fn [core method & args] method))

(defmethod router-base :navigate
  [core _ path-or-route]
  (cond
    (string? path-or-route) (accountant/navigate! path-or-route)
    (vector? path-or-route) (let [path (router-base core :path-for path-or-route)]
                              (accountant/navigate! path))))

(defmethod router-base :match
  [{:keys [reitit-router]} _ path]
  (rfront/match-by-path reitit-router path))

(defmethod router-base :path-for
  [core _ page-args]
  (let [reitit-router (:reitit-router core)]
    (:path (apply rfront/match-by-name reitit-router page-args))))

(defmethod router-base :router-atom
  [core _ _]
  (:router-atom core))

(defn create-router-instance
  [config]
  (let [{:keys [routes dispatch on-navigate-hook]} config
        reitit-router (rfront/router routes
                                     {:compile coercion/compile-request-coercers})
        router-atom (r/atom {})
        router-core {:reitit-router reitit-router
                     :router-atom router-atom}]
    (clerk/initialize!)
    (accountant/configure-navigation!
     {:nav-handler
      (fn [path]
        (let [match (rfront/match-by-path reitit-router path)
              path-params (or (get-in match [:parameters :path])
                              (:path-params match))
              query-params (or (get-in match [:parameters :query])
                               (:query-params match))
              page-name (:name (:data match))
              current-route {:page-name page-name
                             :path path
                             :params (merge path-params query-params)
                             :query-params query-params
                             :path-params path-params}]
          (r/after-render clerk/after-render!)
          (swap! router-atom assoc :current-route current-route)
          (when on-navigate-hook
            (dispatch on-navigate-hook current-route))
          (clerk/navigate-page! path)))
      :path-exists?
      (fn [path]
        (boolean (rfront/match-by-path reitit-router path)))})
    (accountant/dispatch-current!)
    (partial router-base router-core)))

;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [router]} _ request]
  (let [current-route (:current-route @(router :router-atom))]
    (update-in request [:_env] (fn [env] (merge env current-route)))))

(defmethod b/do-base :navigate
  [{:keys [router]} _ path-or-data]
  (router :navigate path-or-data))

(defmethod b/view-base :router-page
  [{:keys [router] :as core} _ _]
  (let [current-route (:current-route @(router :router-atom))
        {:keys [page-name params]} current-route]
    [b/view-base core page-name (assoc params :_env current-route)]))

;; -------------------------
;; Routes

(defmethod ig/init-key :fancoil.system/router
  [_ config]
  (create-router-instance config))

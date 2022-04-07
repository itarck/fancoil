(ns fancoil.modules.html-router
  (:require
   [reagent.core :as r]
   [integrant.core :as ig]
   [reitit.frontend :as rfront]
   [clerk.core :as clerk]
   [accountant.core :as accountant]
   [fancoil.base :as b]))

;; base

(defmulti html-router-base
  (fn [core method & args] method))

(defmethod html-router-base :navigate
  [core _ path]
  (accountant/navigate! path))

(defmethod html-router-base :path-for
  [core _ [page-name params]]
  (let [reitit-router (:reitit-router core)]
    (:path (rfront/match-by-name reitit-router page-name params))))

(defmethod html-router-base :router-atom
  [core _ _]
  (:router-atom core))

(defn create-html-router-instance
  [config]
  (let [{:keys [routes dispatch on-navigate-hook]} config
        reitit-router (rfront/router routes)
        router-atom (r/atom {})
        router-core {:reitit-router reitit-router
                     :router-atom router-atom}]
    (clerk/initialize!)
    (accountant/configure-navigation!
     {:nav-handler
      (fn [path]
        (let [match (rfront/match-by-path reitit-router path)
              page-name (:name (:data  match))
              current-route {:page-name page-name
                             :page-path path
                             :page-params (:path-params match)}]
          (r/after-render clerk/after-render!)
          (swap! router-atom assoc :current-route current-route)
          (when on-navigate-hook
            (dispatch on-navigate-hook {}))
          (clerk/navigate-page! path)))
      :path-exists?
      (fn [path]
        (boolean (rfront/match-by-path reitit-router path)))})
    (accountant/dispatch-current!)
    (partial html-router-base router-core)))

;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [html-router]} _ request]
  (let [current-route (:current-route @(html-router :router-atom))]
    (update-in request [:_env] (fn [env] (merge env current-route)))))

(defmethod b/do-base :navigate
  [{:keys [html-router]} _ {:keys [page-name page-params]}]
  (let [path (html-router :path-for [page-name page-params])]
    (accountant/navigate! path)))

(defmethod b/view-base :router-page
  [{:keys [html-router] :as core} _ _]
  (let [current-route (:current-route @(html-router :router-atom))
        {:keys [page-name page-params]} current-route]
    [b/view-base core page-name (assoc page-params :_env current-route)]))

;; -------------------------
;; Routes

(defmethod ig/init-key :fancoil.system/html-router
  [_ config]
  (create-html-router-instance config))

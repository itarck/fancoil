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
  (let [{:keys [routes dispatch on-navigate-request]} config
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
              current-route (merge
                             (:path-params match)
                             {:_env {:page-name page-name
                                     :page-path path}})]
          (r/after-render clerk/after-render!)
          (swap! router-atom assoc :current-route current-route)
          (when on-navigate-request
            (dispatch on-navigate-request current-route))
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
    (assoc-in request [:_env :current-route] current-route)))

(defmethod b/do-base :navigate
  [{:keys [html-router]} _ {:keys [page-name params]}]
  (let [path (html-router :path-for [page-name params])]
    (accountant/navigate! path)))

(defmethod b/view-base :router-page
  [{:keys [html-router] :as core} _ _]
  (let [current-route-value (:current-route @(html-router :router-atom))
        page-name (get-in current-route-value [:_env :page-name])]
    [b/view-base core page-name current-route-value]))

;; -------------------------
;; Routes

(defmethod ig/init-key :fancoil.system/html-router
  [_ config]
  (create-html-router-instance config))

(ns fancoil.modules.html-router
  (:require
   [reagent.core :as r]
   [integrant.core :as ig]
   [reitit.frontend :as rfront]
   [clerk.core :as clerk]
   [accountant.core :as accountant]

   [fancoil.base :as fancoil.base]))

;; base

(defmulti html-router
  (fn [core method & args] method))

(defmethod html-router :html-router/navigate
  [core _ path]
  (accountant/navigate! path))

(defmethod html-router :html-router/path-for
  [core _ page-name & [params]]
  (let [reitit-router (:reitit-router core)]
    (if params
      (:path (rfront/match-by-name reitit-router page-name params))
      (:path (rfront/match-by-name reitit-router page-name)))))

(defmethod html-router :html-router/current-route-atom
  [core _ _]
  (r/cursor (:router-atom core) [:current-route]))

;; plugin for fancoil

(defmethod fancoil.base/inject :inject.html-router/current-route
  [{:keys [router]} _ request]
  (let [current-route @(router :html-router/current-route-atom)]
    (assoc-in request [:env :current-route] current-route)))

(defmethod fancoil.base/do! :do.router/navigate
  [{:keys [router]} _ {:keys [page-name params]}]
  (let [path (router :html-router/path-for page-name params)]
    (accountant/navigate! path)))

;; -------------------------
;; Routes

(defmethod ig/init-key :system/html-router
  [_ {:keys [routes dispatch on-navigate-request]}]
  (let [reitit-router (rfront/router routes)
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
                             :path-params (:path-params match)}]
          (r/after-render clerk/after-render!)
          (swap! router-atom assoc :current-route current-route)
          (when on-navigate-request
            (dispatch on-navigate-request current-route))
          (clerk/navigate-page! path)))
      :path-exists?
      (fn [path]
        (boolean (rfront/match-by-path reitit-router path)))})
    (accountant/dispatch-current!)
    (partial html-router router-core)))

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
  [core _ page-name & [params]]
  (let [reitit-router (:reitit-router core)]
    (if params
      (:path (rfront/match-by-name reitit-router page-name params))
      (:path (rfront/match-by-name reitit-router page-name)))))

(defmethod html-router-base :current-route-atom
  [core _ _]
  (r/cursor (:router-atom core) [:current-route]))

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
    (partial html-router-base router-core)))

;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [html-router]} _ request]
  (let [current-route @(html-router :current-route-atom)]
    (assoc-in request [:env :current-route] current-route)))

(defmethod b/do-base :navigate
  [{:keys [html-router]} _ {:keys [page-name params]}]
  (let [path (html-router :path-for page-name params)]
    (accountant/navigate! path)))

;; -------------------------
;; Routes

(defmethod ig/init-key :system/html-router
  [_ config]
  (create-html-router-instance config))

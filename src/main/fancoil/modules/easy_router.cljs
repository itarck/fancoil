(ns fancoil.modules.easy-router
  (:require
   [cljs.pprint :refer [pprint]]
   [fancoil.base :as b]
   [integrant.core :as ig]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reitit.frontend :as rf]
   [reitit.frontend.easy :as rfe]
   [reitit.coercion.spec :as rss]
   [spec-tools.data-spec :as ds]))


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

(defn create-router-instance
  [config]
  (let [{:keys [routes]} config
        core {:router-atom (r/atom nil)}]
    (rfe/start!
     (rf/router routes {:data {:coercion rss/coercion}})
     (fn [m] (reset! (:router-atom core) m))
    ;; set to false to enable HistoryAPI
     {:use-fragment true})
    (partial router-base core))
  )

;; plugin for fancoil

(defmethod b/inject-base :current-route
  [{:keys [router]} _ request]
  (let [current-route (:data @(router :router-atom))]
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

(defmethod ig/init-key :fancoil.units/easy-router
  [_ config] 
  (create-router-instance config))


;; ======

;; (defonce match (r/atom nil))

;; (defn current-page []
;;   [:div
;;    [:ul
;;     [:li [:a {:href (rfe/href ::frontpage)} "Frontpage"]]
;;     [:li [:a {:href (rfe/href ::about)} "About"]]
;;     [:li [:a {:href (rfe/href ::item {:id 1})} "Item 1"]]
;;     [:li [:a {:href (rfe/href ::item {:id 2} {:foo "bar"})} "Item 2"]]]
;;    (if @match
;;      (let [view (:name (:data @match))]
;;        [view-base view @match]))
;;    [:pre (with-out-str (pprint @match))]])

;; (def routes
;;   [["/"
;;     {:name ::frontpage}]

;;    ["/about"
;;     {:name ::about}]

;;    ["/item/:id"
;;     {:name ::item
;;      :parameters {:path {:id int?}
;;                   :query {(ds/opt :foo) keyword?}}}]])

;; (defn init! []
;;   (rfe/start!
;;    (rf/router routes {:data {:coercion rss/coercion}})
;;    (fn [m] (reset! match m))
;;     ;; set to false to enable HistoryAPI
;;    {:use-fragment true})
;;   (rdom/render [current-page] (.getElementById js/document "app")))

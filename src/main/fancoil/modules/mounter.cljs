(ns fancoil.modules.mounter
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [fancoil.base :as b :refer [pager-base do-base nav-base]]
   [integrant.core :as ig]))


;; pager


(defmethod pager-base :get-state
  [{:keys [state*]} _]
  state*)

(defmethod pager-base :cursor-current-page
  [{:keys [state*]} _]
  (r/cursor state* [:current-page]))


(defmethod pager-base :change-page
  [{:keys [state* cache] :as core} _ {:keys [page]}]
  (swap! state* assoc :current-page page)
  (swap! state* update :history-pages (fn [pages]
                                        (vec (take 10 (conj pages page)))))
  (reset! cache {:timestamp (js/Date.)})

  (when (get (methods pager-base) (first page))
    (let [hooks (apply pager-base core page)
          {:keys [on-load-hook]} hooks]
      (when on-load-hook
        (on-load-hook)))))


(defmethod ig/init-key :fancoil.units/pager
  [_ config]
  (let [{:keys [initial-page]} config
        core (assoc config :state* (r/atom {:current-page initial-page
                                            :history-pages []}))]
    (partial pager-base core)))

(defmethod do-base :change-page
  [{:keys [pager]} _ page]
  (pager :change-page {:page page}))

;; nav 

(defmethod nav-base :default
  [{:keys [pager]} method & args]
  (println (concat [method] args))
  (pager :change-page {:page (concat [method] args)}))

(defmethod ig/init-key :fancoil.units/nav
  [_ config]
  (partial nav-base config))

;; mounter

(defmulti mounter-base
  (fn [core method & args] method))

(defmethod mounter-base :root-page
  [{:keys [view pager]} _]
  (fn []
    (let [page @(pager :cursor-current-page)]
      (vec (concat [view] page)))))

(defmethod mounter-base :mount!
  [{:keys [dom-id] :as core} _]
  (rdom/render
   [(mounter-base core :root-page)]
   (.getElementById js/document dom-id)))


(defmethod ig/init-key :fancoil.units/mounter
  [_ config]
  (let [mounter-unit (partial mounter-base config)]
    (mounter-unit :mount!)
    mounter-unit))



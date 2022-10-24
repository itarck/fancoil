(ns fancoil.modules.mounter
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [fancoil.base :as b]
   [integrant.core :as ig]))


;; pager: 

(defmulti pager-base
  (fn [core method & args] method))

(defmethod pager-base :get-state
  [{:keys [state*]} _]
  state*)

(defmethod pager-base :cursor-current-page
  [{:keys [state*]} _]
  (r/cursor state* [:current-page]))

(defmethod pager-base :cursor-cache
  [{:keys [state*]} _]
  (r/cursor state* [:cache]))

(defmethod pager-base :change-page
  [{:keys [state*]} _ {:keys [page]}]
  (swap! state* assoc :current-page page)
  (swap! state* assoc :cache {:timestamp (js/Date.)})
  (swap! state* update :history-pages (fn [pages] 
                                        (vec (take 10 (conj pages page))))))


(defmethod ig/init-key :fancoil.units/pager
  [_ config]
  (let [{:keys [initial-page]} config
        core {:state* (r/atom {:current-page initial-page
                               :history-pages []
                               :cache {}})}]
    (partial pager-base core)))


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



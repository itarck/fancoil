(ns fancoil.modules.mounter
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [fancoil.base :as b :refer [pager-base]]
   [integrant.core :as ig]))


;; pager: 


(defmethod pager-base :get-state
  [{:keys [state*]} _]
  state*)

(defmethod pager-base :cursor-current-page
  [{:keys [state*]} _]
  (r/cursor state* [:current-page]))


(defmethod pager-base :change-page
  [{:keys [state* cache dispatch] :as core} _ {:keys [page]}]
  (swap! state* assoc :current-page page)
  (swap! state* update :history-pages (fn [pages]
                                        (vec (take 10 (conj pages page)))))
  (reset! cache {:timestamp (js/Date.)})
  
  (when (get (methods pager-base) (first page))
    (let [hooks (apply pager-base core page)
          {:keys [on-load]} hooks]
      (when on-load
        (apply dispatch on-load)))))


(defmethod ig/init-key :fancoil.units/pager
  [_ config]
  (let [{:keys [initial-page dispatch cache]} config
        core {:state* (r/atom {:current-page initial-page
                               :history-pages []})
              :dispatch dispatch
              :cache cache}]
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



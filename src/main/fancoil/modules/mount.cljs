(ns fancoil.modules.mount
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [fancoil.base :as b :refer [page-base do-base nav-base]]
   [integrant.core :as ig]))


;; page


(defmethod page-base :get-state
  [{:keys [state*]} _]
  state*)

(defmethod page-base :cursor-current-page
  [{:keys [state*]} _]
  (r/cursor state* [:current-page]))


(defmethod page-base :change-page
  [{:keys [state* cache] :as core} _ {:keys [page]}]
  (swap! state* assoc :current-page page)
  (swap! state* update :history-pages (fn [pages]
                                        (vec (take 10 (conj pages page)))))
  (reset! cache {:timestamp (js/Date.)})

  (when (get (methods page-base) (first page))
    (let [hooks (apply page-base core page)
          {:keys [on-load-hook]} hooks]
      (when on-load-hook
        (on-load-hook)))))


(defmethod ig/init-key :fancoil.units/page
  [_ config]
  (let [{:keys [initial-page]} config
        core (assoc config :state* (r/atom {:current-page initial-page
                                            :history-pages []}))]
    (partial page-base core)))

(defmethod do-base :change-page
  [{:keys [page]} _ page]
  (page :change-page {:page page}))

;; nav 

(defmethod nav-base :default
  [{:keys [page]} method & args]
  (println (concat [method] args))
  (page :change-page {:page (concat [method] args)}))

(defmethod ig/init-key :fancoil.units/nav
  [_ config]
  (partial nav-base config))

;; mount

(defmulti mount-base
  (fn [core method & args] method))

(defmethod mount-base :root-page
  [{:keys [view page]} _]
  (fn []
    (let [page @(page :cursor-current-page)]
      (vec (concat [view] page)))))

(defmethod mount-base :mount!
  [{:keys [dom-id] :as core} _]
  (rdom/render
   [(mount-base core :root-page)]
   (.getElementById js/document dom-id)))


(defmethod ig/init-key :fancoil.units/mount
  [_ config]
  (let [mount-unit (partial mount-base config)]
    (mount-unit :mount!)
    mount-unit))



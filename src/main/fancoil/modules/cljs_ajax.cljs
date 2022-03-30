(ns fancoil.modules.cljs-ajax
  (:require
   [cljs.core.async :refer [go]]
   [fancoil.base :as base]
   [ajax.core :refer [json-request-format json-response-format GET POST PUT DELETE]]
   [ajax.simple :refer [ajax-request]]))


;; helers

(def default-request
  {:format          (json-request-format)
   :response-format (json-response-format {:keywords? true})})


;; base functions

(defmethod base/do! :do.ajax/request
  [core _ request]
  (go
    (let [{:keys [on-success on-failure] :or {on-failure #(js/console.error %)}} request
          on-success-handler (if (fn? on-success)
                               on-success
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method req]]
                                                                [method (assoc req :event response)])
                                                              on-success)]
                                   (base/do! core :do.dispatch/actions injected-actions))))

          on-failure-handler (if (fn? on-failure)
                               on-failure
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method req]]
                                                                [method (assoc req :event response)])
                                                              on-success)]
                                   (base/do! core :do.dispatch/actions injected-actions))))
          handler (fn [[ok response]]
                    (if ok
                      (on-success-handler response)
                      (on-failure-handler response)))
          merged-request (->
                          (merge default-request request)
                          (assoc :handler handler))]
      (ajax-request merged-request))))


(derive :do.ajax/get :do.ajax/easy-request)
(derive :do.ajax/post :do.ajax/easy-request)
(derive :do.ajax/put :do.ajax/easy-request)
(derive :do.ajax/delete :do.ajax/easy-request)

(defmethod base/do! :do.ajax/easy-request
  [core method effect]
  (go
    (let [{:keys [uri opt on-success on-failure]
           :or {opt {} on-failure #(js/console.error %)}
           on-finally :finally} effect
          on-success-handler (if (fn? on-success)
                               on-success
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-success)]
                                   (base/do! core :do.dispatch/actions injected-actions))))

          on-failure-handler (if (fn? on-failure)
                               on-failure
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-failure)]
                                   (base/do! core :do.dispatch/actions injected-actions))))
          on-finally-handler (if (fn? on-finally)
                               on-finally
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-finally)]
                                   (base/do! core :do.dispatch/actions injected-actions))))
          opt (-> opt
                  (assoc :handler on-success-handler
                         :error-handler on-failure-handler)
                  ((fn [opt]
                     (if on-finally-handler
                       (assoc opt :finally on-finally-handler)
                       opt))))]
      (case method
        :do.ajax/get (GET uri opt)
        :do.ajax/post (POST uri opt)
        :do.ajax/put (PUT uri opt)
        :do.ajax/delete (DELETE uri opt)))))

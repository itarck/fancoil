(ns fancoil.modules.cljs-ajax
  (:require
   [cljs.core.async :refer [go]]
   [fancoil.units :as fu]
   [ajax.core :refer [json-request-format json-response-format GET POST PUT DELETE]]
   [ajax.simple :refer [ajax-request]]))


;; helers

(def default-request
  {:format          (json-request-format)
   :response-format (json-response-format {:keywords? true})})


;; base functions

(defmethod fu/do-base :ajax-request
  [core _ request]
  (go
    (let [{:keys [on-success on-failure] :or {on-failure #(js/console.error %)}} request
          on-success-handler (if (fn? on-success)
                               on-success
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method req]]
                                                                [method (assoc req :event response)])
                                                              on-success)]
                                   (fu/do-base core :dispatch-many injected-actions))))

          on-failure-handler (if (fn? on-failure)
                               on-failure
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method req]]
                                                                [method (assoc req :event response)])
                                                              on-success)]
                                   (fu/do-base core :dispatch-many injected-actions))))
          handler (fn [[ok response]]
                    (if ok
                      (on-success-handler response)
                      (on-failure-handler response)))
          merged-request (->
                          (merge default-request request)
                          (assoc :handler handler))]
      (ajax-request merged-request))))


(derive :ajax-get :ajax-easy-request)
(derive :ajax-post :ajax-easy-request)
(derive :ajax-put :ajax-easy-request)
(derive :ajax-delete :ajax-easy-request)

(defmethod fu/do-base :ajax-easy-request
  [core method easy-request]
  (go
    (let [{:keys [uri on-success on-failure]
           :or {on-failure #(js/console.error %)}
           on-finally :finally} easy-request
          on-success-handler (if (fn? on-success)
                               on-success
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-success)]
                                   (fu/do-base core :dispatch-many injected-actions))))

          on-failure-handler (if (fn? on-failure)
                               on-failure
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-failure)]
                                   (fu/do-base core :dispatch-many injected-actions))))
          on-finally-handler (if (fn? on-finally)
                               on-finally
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (assoc request :event response)])
                                                              on-finally)]
                                   (fu/do-base core :dispatch-many injected-actions))))
          easy-request (-> easy-request
                           (assoc :handler on-success-handler
                                  :error-handler on-failure-handler)
                           ((fn [easy-request]
                              (if on-finally-handler
                                (assoc easy-request :finally on-finally-handler)
                                easy-request))))]
      (case method
        :ajax-get (GET uri easy-request)
        :ajax-post (POST uri easy-request)
        :ajax-put (PUT uri easy-request)
        :ajax-delete (DELETE uri easy-request)))))

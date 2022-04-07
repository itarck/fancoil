(ns fancoil.modules.cljs-ajax
  (:require
   [cljs.core.async :refer [go]]
   [fancoil.base :as b]
   [ajax.core :refer [json-request-format json-response-format GET POST PUT DELETE]]
   [ajax.simple :refer [ajax-request]]))


;; helers

(def default-request
  {:format          (json-request-format)
   :response-format (json-response-format {:keywords? true})})


;; base functions

(defmethod b/do-base :ajax-request
  [core _ request]
  (go
    (let [{:keys [on-success on-failure] :or {on-failure #(js/console.error %)}} request
          on-success-handler (if (fn? on-success)
                               on-success
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (merge request response)])
                                                              on-success)]
                                   (b/do-base core :dispatch-many injected-actions))))

          on-failure-handler (if (fn? on-failure)
                               on-failure
                               (fn [response]
                                 (let [injected-actions (mapv (fn [[method request]]
                                                                [method (merge request response)])
                                                              on-success)]
                                   (b/do-base core :dispatch-many injected-actions))))
          handler (fn [[ok response]]
                    (if ok
                      (on-success-handler response)
                      (on-failure-handler response)))
          merged-request (->
                          (merge default-request request)
                          (assoc :handler handler))]
      (ajax-request merged-request))))


(defn gen-easy-request
  [core method easy-request]
  (let [{:keys [uri on-success on-failure]
         :or {on-failure #(js/console.error %)}
         on-finally :finally} easy-request
        on-success-handler (if (fn? on-success)
                             on-success
                             (fn [response]
                               (let [injected-actions (mapv (fn [[method request]]
                                                              [method (merge request response)])
                                                            on-success)]
                                 (b/do-base core :dispatch-many injected-actions))))

        on-failure-handler (if (fn? on-failure)
                             on-failure
                             (fn [response]
                               (let [injected-actions (mapv (fn [[method request]]
                                                              [method (merge request response)])
                                                            on-failure)]
                                 (b/do-base core :dispatch-many injected-actions))))
        on-finally-handler (if (fn? on-finally)
                             on-finally
                             (fn [response]
                               (let [injected-actions (mapv (fn [[method request]]
                                                              [method (merge request response)])
                                                            on-finally)]
                                 (b/do-base core :dispatch-many injected-actions))))
        easy-request (-> easy-request
                         (assoc :handler on-success-handler
                                :error-handler on-failure-handler)
                         ((fn [easy-request]
                            (if on-finally-handler
                              (assoc easy-request :finally on-finally-handler)
                              easy-request))))]
    easy-request))

(defmethod b/do-base :ajax-get
  [core method easy-request]
  (let [{:keys [uri]} easy-request]
    (go
      (GET uri (gen-easy-request core method easy-request)))))

(defmethod b/do-base :ajax-post
  [core method easy-request]
  (let [{:keys [uri]} easy-request]
    (go
      (POST uri (gen-easy-request core method easy-request)))))

(defmethod b/do-base :ajax-put
  [core method easy-request]
  (let [{:keys [uri]} easy-request]
    (go
      (PUT uri (gen-easy-request core method easy-request)))))

(defmethod b/do-base :ajax-delete
  [core method easy-request]
  (go
    (let [{:keys [uri]} easy-request]
      (DELETE uri (gen-easy-request core method easy-request)))))



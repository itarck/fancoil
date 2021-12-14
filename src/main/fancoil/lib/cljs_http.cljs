(ns fancoil.lib.cljs-http
  (:require
   [cljs.core.async :refer [go <!]]
   [cljs-http.core :as cljs-http]
   [cljs-http.client :as client]
   [fancoil.base :as base]))


(defmethod base/do! :cljs-http/request
  [{:keys [dispatch]} _ ring-request]
  (let [{:keys [callback request-method uri]} ring-request]
    (go
      (let [response (<! (cljs-http/request ring-request))]
        (dispatch callback response)))))



(comment
  (base/do! {:dispatch println} :cljs-http/request {:scheme :https
                                                    :request-method :get
                                                    :server-name "news.ycombinator.com"
                                                    :uri "/newest"
                                                    :callback :a-callback}))


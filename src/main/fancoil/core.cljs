(ns fancoil.core
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [cljs.spec.alpha :as s]
   [clojure.string :as string]
   [posh.reagent :as p]
   [datascript.core :as d]
   [integrant.core :as ig]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [medley.core :as m]

   [fancoil.base :as base]
   [fancoil.spec]

   [fancoil.modules.datascript]
   [fancoil.modules.cljs-ajax]))

;; helper function

(defn parse-module-name
  [kw]
  (let [ns1 (namespace kw)]
    (first (string/split ns1 #"\."))))

(defn load-hierarchy
  [hierarchy]
  (doseq [[tag parents] hierarchy
          parent parents]
    (derive tag parent)))

(defn merge-config
  [old-config new-config]
  (merge-with merge old-config new-config))


;; base function

(def model base/model)
(def handle base/handle)
(def inject base/inject)
(def do! base/do!)
(def process base/process)
(def subscribe base/subscribe)
(def view base/view)
(def schedule base/schedule)
(def spec base/spec)
(def schema base/schema)
(def system base/system)


;; ------------------------------------------------
;; value

(defmethod ig/init-key :system/value
  [_ config]
  config)

;; ------------------------------------------------
;; ratom

(defmethod ig/init-key :system/ratom
  [_ config]
  (let [{:keys [initial-value]} config]
    (r/atom initial-value)))

;; ------------------------------------------------
;; schema
;; 

(defmethod ig/init-key :system/schema
  [_ _]
  (let [schema-ref (atom {})]
    (doseq [[k f] (methods base/schema)]
      (let [schm (f)]
        (swap! schema-ref merge schm)))
    @schema-ref))

;; ------------------------------------------------
;; spec 

(defmethod base/spec :spec/assert
  [_ _ spec data]
  (s/assert spec data))

(defmethod base/spec :spec/valid?
  [_ _ spec data]
  (s/valid? spec data))

(defmethod ig/init-key :system/spec
  [_ _]
  (let [ms (methods base/spec)]
    (doseq [[k f] ms]
      (when (and
             (qualified-keyword? k)
             (not= (namespace k) "spec"))
        (f)))
    (s/check-asserts true)
    (partial base/spec {})))


;; ------------------------------------------------

;; pconn

(defmethod ig/init-key :system/pconn
  [_k config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    (p/posh! conn)
    conn))

;; ------------------------------------------------
;; subscribe

(defmethod base/subscribe :subscribe/pull
  [{:keys [pconn]} _ {:keys [selector id]}]
  (p/pull pconn selector id))

(defmethod base/subscribe :subscribe/q
  [{:keys [pconn]} _ {:keys [query inputs]}]
  (apply p/q query (concat [pconn] inputs)))

(defmethod ig/init-key :system/subscribe
  [_ config]
  (fn [method signal]
    (let [core config]
      (try
        (s/assert :subscribe/input signal)
        (let [output (base/subscribe core method signal)]
          (s/assert :subscribe/output output)
          output)
        (catch js/Object e (println "error in system/subscribe: " (str e)))))))

;; ------------------------------------------------
;; view

(defmethod ig/init-key :system/view
  [_ config]
  (fn [method scope]
    (let [core config]
      (try
        (s/assert :view/input scope)
        (let [output (base/view core method scope)]
          (s/assert :view/output output)
          output)
        (catch js/Object e (println "error in system/view: " (str e)))))))

;; ------------------------------------------------
;; model

(defmethod ig/init-key :system/model
  [_ config]
  (partial base/model config))


;; ------------------------------------------------
;; handle

(defmethod ig/init-key :system/handle
  [_ config]
  (fn [method req]
    (let [core config]
      (try
        (s/assert :handle/input req)
        (let [output (base/handle core method req)]
          (s/assert :handle/output output)
          output)
        (catch js/Object e (println "error in system/handle: " (str e)
                                    "method: " method
                                    "request: " (str req)))))))


;; ------------------------------------------------
;; schedule

(defmethod ig/init-key :system/schedule
  [_ config]
  (partial base/schedule config))

;; ------------------------------------------------
;; chan

(defmethod ig/init-key :system/chan
  [_ _]
  (chan))

;; ------------------------------------------------
;; dispatch

(defmethod ig/init-key :system/dispatch
  [_ {:keys [out-chan]}]
  (fn dispatch
    [method request]
    (go (>! out-chan [method request]))))

;; ------------------------------------------------
;; inject

(defmethod base/inject :inject/inject-all
  [{:keys [inject-keys] :as core} _ request]
  (reduce (fn [req k]
            (let [n (name k)]
              (base/inject core k req)))
          request inject-keys))

(defmethod base/inject :inject.posh/db
  [{:keys [pconn]} _method req]
  (assoc-in req [:env :db] @pconn))

(defmethod inject :inject.ratom/db
  [{:keys [ratom]} _method req]
  (assoc-in req [:env :db] @ratom))

(defmethod ig/init-key :system/inject
  [_ config]
  (fn [method req]
    (let [core config]
      (try
        (s/assert :inject/input req)
        (let [output (base/inject core method req)]
          (s/assert :inject/output output)
          output)
        (catch js/Object e (println (str e)))))))

;; ------------------------------------------------
;; do 

(defmethod base/do! :do.dispatch/action
  [{:keys [dispatch]} _ action]
  (let [[method request] action]
    (dispatch method request)))

(defmethod base/do! :do.dispatch/actions
  [{:keys [dispatch]} _ actions]
  (doseq [[method request] actions]
    (dispatch method request)))

(defmethod base/do! :do.log/out
  [_ _ value]
  (println "do.log/out: " value))

(defmethod base/do! :do.log/error
  [_ _ value]
  (println "do.log/error: " value))

(defmethod base/do! :do.posh/tx
  [{:keys [pconn]} _ tx]
  (p/transact! pconn tx)
  tx)


(defmethod do! :do.ratom/reset
  [{:keys [ratom]} _ db-value]
  (reset! ratom db-value))

(defmethod do! :do.ratom/set-paths
  [{:keys [ratom]} _ path-value-pairs]
  (doseq [[path value] path-value-pairs]
    (swap! ratom assoc-in path value)))

(defmethod do! :do.ratom/delete-paths
  [{:keys [ratom]} _ paths]
  (doseq [path paths]
    (swap! ratom m/dissoc-in path)))


(defmethod base/do! :do.stdout/put
  [{:keys [stdout-chan]} _ data]
  (go (>! stdout-chan data)))


(defmethod base/do! :do.container/dispatch
  [{:keys [container-system]} _ {:request/keys [method body]}]
  (let [dispatch (container-system :get-unit {:unit-name :dispatch})]
    (dispatch method body)))

(defmethod ig/init-key :system/do!
  [_ config]
  (partial base/do! config))


;; ------------------------------------------------
;; process

(defmethod base/process :default
  [{:keys [do! handle inject] :as core} method req]
  (let [module-name (parse-module-name method)]
    (cond
      (= module-name "do") (do! method req)
      (= module-name "handle") (let [req (inject :inject/inject-all req)
                                     effect (handle method req)]
                                 (doseq [[k v] effect]
                                   (let [effect-method-name (parse-module-name k)]
                                     (cond
                                       (= effect-method-name "do") (do! k v)
                                       (= effect-method-name "handle") (base/process core k v)
                                       :else (println "effect not allowed")))))
      :else (println "process method not allowed" (str method req)))))

(defmethod ig/init-key :system/process
  [_ config]
  (partial base/process config))


;; ------------------------------------------------
;; service

(defmethod ig/init-key :system/service
  [_ {:keys [process in-chan]}]
  (go-loop []
    (let [[method request] (<! in-chan)]
      (if (get-in request [:props :sync?])
        (process method request)
        (go (process method request))))
    (recur))
  {:in-chan in-chan})


;; ------------------------------------------------
;; system


(defn create-system
  [{:keys [config view-method view-ctx]}]
  (let [core {:entry-atom (r/atom {:view-method view-method
                                   :view-ctx view-ctx})
              :instance-atom (r/atom (ig/init config))}]
    (fn [method & args]
      (apply base/system core method args))))

(defmethod base/system :system/get-instance
  [core _ _]
  (let [instance @(:instance-atom core)]
    instance))

(defmethod base/system :system/get-unit
  [core _ {:keys [unit-name]}]
  (let [instance (base/system core :system/get-instance {})]
    (unit-name instance)))


(defmethod base/system :system/mount
  [{:keys [entry-atom] :as core} _ {:keys [page-element-id]}]
  (let [view-fn (base/system core :system/get-unit {:unit-name :system/view})
        root-view (fn []
                    (let [{:keys [view-method view-ctx]} @entry-atom]
                      [view-fn view-method view-ctx]))]
    (rdom/render
     [root-view]
     (.getElementById js/document page-element-id))))

(defmethod base/system :system/dispatch
  [core _ [method request]]
  (let [dispatch-fn (base/system core :system/get-unit {:unit-name :system/dispatch})]
    (dispatch-fn method request)))


(defmethod base/system :system/change-view
  [{:keys [entry-atom]} _ [view-method view-ctx]]
  (reset! entry-atom {:view-method view-method
                      :view-ctx view-ctx}))

(defmethod base/system :default
  [core method ctx]
  (let [module-name (parse-module-name method)]
    (case module-name
      "view" (base/system core :system/change-view [method ctx])
      "handle" (base/system core :system/dispatch [method ctx])
      nil)))


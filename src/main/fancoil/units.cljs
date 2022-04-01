(ns fancoil.units
  (:require
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [cljs.spec.alpha :as s]
   [datascript.core :as d]
   [posh.reagent :as p]
   [medley.core :as m]
   [reagent.core :as r]
   [reagent.ratom :as ra]))


;; ctx spec

(s/def ::method keyword?)
(s/def ::request map?)
(s/def ::action (s/cat :method ::method
                       :request ::request))
(s/def ::event some?)
(s/def ::env map?)

(s/def ::effect (s/coll-of (s/cat :method ::method
                                  :fx some?)))
(s/def ::scope map?)
(s/def ::signal map?)
(s/def ::hiccup vector?)
(s/def ::props map?)

(defn- spec-exception [method k v spec explain-data]
  (ex-info (str "Spec failed in " method ":  "
                (with-out-str (s/explain-out explain-data)))
           {:reason   ::spec-check-failed
            :method method
            :key      k
            :value    v
            :spec     spec
            :explain  explain-data}))

(defn- assert-spec [method spec value]
  (when-not (s/valid? spec value)
    (throw
     (spec-exception method key value spec (s/explain-data spec value)))))

(s/check-asserts true)

;; ------------------------------------------------
;; spec 

(defmulti spec-base
  (fn [core method & args] method))

(defmethod spec-base :assert
  [_ method spec data]
  (assert-spec method spec data))

(defmethod spec-base :valid?
  [_ _ spec data]
  (s/valid? spec data))

(defn create-spec-instance
  [config]
  (let [ms (methods spec-base)]
    (doseq [[k f] ms]
      (when (and
             (qualified-keyword? k)
             (not= (namespace k) "spec"))
        (f)))
    (partial spec-base {})))

;; ------------------------------------------------
;; ratom

(defn create-ratom
  [config]
  (let [{:keys [initial-value]} config]
    (r/atom initial-value)))

;; ------------------------------------------------
;; poshed-ds

(defn create-poshed-datascript-conn
  [config]
  (let [{:keys [schema initial-tx initial-db] :or {schema {}}} config
        conn (d/create-conn schema)]
    (when initial-db
      (d/reset-conn! conn initial-db))
    (when initial-tx
      (d/transact! conn initial-tx))
    (p/posh! conn)
    conn))

;; ------------------------------------------------
;; datascript schema

(defmulti schema-base
  (fn [config signal & args] signal))

(defn create-schema-instance
  [config]
  (let [schema-ref (atom {})]
    (doseq [[k f] (methods schema-base)]
      (let [schm (f)]
        (swap! schema-ref merge schm)))
    @schema-ref))

;; ------------------------------------------------
;; subscribe

(defmulti subscribe-base
  "stateful function: subscribe a ratom or reaction
   reaction or ratom in -> reaction out
   core: db-ref
   "
  (fn [core method & args] method))

(defmethod subscribe-base :pull
  [{:keys [pconn]} _ {:keys [selector id]}]
  (p/pull pconn selector id))

(defmethod subscribe-base :q
  [{:keys [pconn]} _ {:keys [query inputs]}]
  (apply p/q query (concat [pconn] inputs)))

(s/def ::subscribe.config map?)
(s/def ::subscribe.input map?)
(s/def ::subscribe.output (fn [t] (or 
                                   (= ra/Reaction (type t))
                                   (= ra/RCursor (type t)))))

(defn create-subscribe-instance
  [config]
  (fn [method signal]
    (let [core config]
      (try
        (assert-spec method ::subscribe.input signal)
        (let [output (subscribe-base core method signal)]
          (assert-spec method ::subscribe.output output)
          output)
        (catch js/Object e (println "error in subscribe unit: " e))))))

;; ------------------------------------------------
;; view

(defmulti view-base
  "stateful function: view a entity
   props in -> reagent views
   core: subscribe, dispatch
   "
  (fn [core method & args] method))


(s/def ::view.config map?)
(s/def ::view.method ::method)
(s/def ::view.input ::scope)
(s/def ::view.output some?)

(defn create-view-instance
  [config]
  (fn [method scope]
    (let [core config]
      (try
        (assert-spec method ::view.input scope)
        (let [output (view-base core method scope)]
          (assert-spec method ::view.output output)
          output)
        (catch js/Object e (println "error in view unit: " e))))))


;; ------------------------------------------------
;; dispatch 

(s/def ::dispatch.input ::request)

(defn create-dispatch-instance
  [config]
  (let [{:keys [out-chan]} config]
    (fn [method request]
      (go (>! out-chan [method request])))))


;; ------------------------------------------------
;; inject

(defmulti inject-base
  "stateful function: inject a cofx
   request in -> request out
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmethod inject-base :inject-all
  [{:keys [inject-keys] :as core} _ request]
  (reduce (fn [req k]
            (inject-base core k req))
          request inject-keys))

(defmethod inject-base :posh-db
  [{:keys [pconn]} _method req]
  (assoc-in req [:env :db] @pconn))

(defmethod inject-base :ratom-db
  [{:keys [ratom]} _method req]
  (assoc-in req [:env :ratom-db] @ratom))

(s/def ::inject.input (s/keys :opt-un [::env ::event]))
(s/def ::inject.output (s/keys :req-un [::env]))

(defn create-inject-instance
  [config]
  (fn [method req]
    (let [core config]
      (try
        (assert-spec method ::inject.input req)
        (let [output (inject-base core method req)]
          (assert-spec method ::inject.output output)
          output)
        (catch js/Object e (println (str e)))))))

;; ------------------------------------------------
;; model

(defmulti model-base
  "pure function: tap a model
   value in -> value out
   "
  (fn [core method & args] method))

(defn create-model-instance
  [config]
  (partial model-base config))

;; ------------------------------------------------
;; handle

(defmulti handle-base
  "pure function: handle a request
   request in -> response out
   {:db db} -> {:tx tx}"
  (fn [core method & args] method))

(s/def ::handle.input (s/keys :req-un [::env] :opt-un [::event]))
(s/def ::handle.output ::effect)

(defn create-handle-instance
  [config]
  (fn [method req]
    (let [core config]
      (try
        (assert-spec method ::handle.input req)
        (let [output (handle-base core method req)]
          (assert-spec method ::handle.output output)
          output)
        (catch js/Object e (println "error in handle unit: " e))))))

;; ------------------------------------------------
;; do 

(defmulti do-base
  "stateful function: do a fx
   response in -> do effects
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmethod do-base :dispatch
  [{:keys [dispatch]} _ action]
  (let [[method request] action]
    (dispatch method request)))

(defmethod do-base :dispatch-many
  [{:keys [dispatch]} _ actions]
  (doseq [[method request] actions]
    (dispatch method request)))

(defmethod do-base :log-out
  [_ _ value]
  (println "log-out: " value))

(defmethod do-base :log-error
  [_ _ value]
  (println "log-error: " value))

(defmethod do-base :tx
  [{:keys [pconn]} _ tx]
  (p/transact! pconn tx)
  tx)

(defmethod do-base :reset-ratom
  [{:keys [ratom]} _ db-value]
  (reset! ratom db-value))

(defmethod do-base :set-ratom-paths
  [{:keys [ratom]} _ path-value-pairs]
  (doseq [[path value] path-value-pairs]
    (swap! ratom assoc-in path value)))

(defmethod do-base :delete-ratom-paths
  [{:keys [ratom]} _ paths]
  (doseq [path paths]
    (swap! ratom m/dissoc-in path)))

(defmethod do-base :stdout
  [{:keys [stdout-chan]} _ data]
  (go (>! stdout-chan data)))


(defn create-do-instance
  [config]
  (fn [method req]
    (let [core config]
      (do-base core method req))))


;; ------------------------------------------------
;; process

(defmulti process-base
  "stateful function: process a request to fx
   request in -> effects
   core: ratom, other resources
   "
  (fn [core method & args] method))

(defmethod process-base :default
  [{:keys [do! handle inject] :as core} method req]
  (if (qualified-keyword? method)
    (let [req (inject :inject-all req)
          effect (handle method req)]
      (doseq [[k v] effect]
        (if (qualified-keyword? k)
          (process-base core k v)
          (do! k v))))
    (do! method req)))

(s/def ::process.input (s/keys :req-un [::event]
                               :opt-un [::env]))

(defn create-process-instance 
  [config]
  (fn [method req]
    (let [core config]
      (try
        (assert-spec method ::do.input req)
        (let [output (process-base core method req)]
          output)
        (catch js/Object e (println "error in process: " e)))))
  (partial process-base config))


;; ------------------------------------------------
;; service

(defn create-service-instance
  [config]
  (let [{:keys [process in-chan]} config]
    (go-loop []
      (let [[method request] (<! in-chan)]
        (if (get-in request [:props :sync?])
          (process method request)
          (go (process method request))))
      (recur))
    {:in-chan in-chan}))


;; ------------------------------------------------
;; schedule

(defmulti schedule-base
  "stateful function: schedule a task once or periodic
   task in -> event out
   core: dispatch
   "
  (fn [core method & args] method))

(defn create-schedule-instance
  [config]
  (partial schedule-base config))


;; workaround for old codes

(def spec spec-base)
(def schema schema-base)
(def handle handle-base)
(def subscribe subscribe-base)
(def view view-base)
(def model model-base)
(def process process-base)
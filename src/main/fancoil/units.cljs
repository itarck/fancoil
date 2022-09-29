(ns fancoil.units
  (:require
   [fancoil.base :refer [spec-base schema-base subscribe-base view-base 
                         inject-base model-base handle-base do-base
                         process-base schedule-base component-base]]
   [cljs.core.async :refer [go go-loop >! <! chan]]
   [cljs.spec.alpha :as s]
   [cljs.pprint :refer [pprint]]
   [datascript.core :as d]
   [integrant.core :as ig]
   [posh.reagent :as p]
   [medley.core :as m]
   [reagent.core :as r]
   [reagent.ratom :as ra]))


;; ------------------------------------------------
;; value 

(defmethod ig/init-key ::value
  [_ config]
  config)

;; ------------------------------------------------
;; spec 

(s/def ::method keyword?)
(s/def ::request map?)
(s/def ::action (s/cat :method ::method
                       :request ::request))
(s/def ::event some?)
(s/def ::_env map?)

(s/def ::effect (s/coll-of (s/cat :method ::method
                                  :fx some?)))
(s/def ::scope map?)
(s/def ::signal map?)
(s/def ::hiccup vector?)
(s/def ::_props map?)

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

(defmethod spec-base :assert
  [_ method spec data]
  (assert-spec method spec data))

(defmethod spec-base :valid?
  [_ _ spec data]
  (s/valid? spec data))

(defmethod ig/init-key ::spec
  [_ config]
  (let [ms (methods spec-base)]
    (doseq [[k f] ms]
      (when (and
             (qualified-keyword? k)
             (not= (namespace k) "spec"))
        (f)))
    (fn spec [method]
      (let [core (assoc config :spec spec)]
        (try
          (let [output (spec-base core method)]
            output)
          (catch js/Object e (println "error in spec unit: " e)))))))

;; ------------------------------------------------
;; ratom

(defn create-ratom
  [config]
  (let [{:keys [initial-value]} config]
    (r/atom initial-value)))

(defmethod ig/init-key ::ratom
  [_ config]
  (create-ratom config))

;; ------------------------------------------------
;; datascript schema


(defn create-schema-instance
  [config]
  (let [schema-ref (atom {})]
    (doseq [[k f] (methods schema-base)]
      (let [schm (f)]
        (swap! schema-ref merge schm)))
    @schema-ref))

(defmethod ig/init-key ::schema
  [_ config]
  (create-schema-instance config))

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

(defmethod ig/init-key ::pconn
  [_k config]
  (create-poshed-datascript-conn config))


;; ------------------------------------------------
;; subscribe

(defmethod subscribe-base :pull
  [{:keys [pconn]} _ {:keys [selector id] :or {selector '[*]}}]
  (p/pull pconn selector id))

(defmethod subscribe-base :pull-many
  [{:keys [pconn]} _ {:keys [selector ids] :or {selector '[*]}}]
  (r/reaction
   (doall (mapv (fn [id] @(p/pull pconn selector id)) ids))))

(defmethod subscribe-base :q
  [{:keys [pconn]} _ {:keys [query inputs]}]
  (apply p/q query (concat [pconn] inputs)))

(s/def ::subscribe.config map?)
(s/def ::subscribe.output (fn [t] (or 
                                   (= cljs.core/Atom (type t))
                                   (= ra/Reaction (type t))
                                   (= ra/RCursor (type t)))))

(defmethod subscribe-base :cursor-ratom
  [{:keys [ratom]} _ {:keys [path]}]
  (r/cursor ratom path))

(defn create-subscribe-instance
  [config]
  (fn subscribe [method signal]
    (let [core (assoc config :subscribe subscribe)]
      (try
        (let [output (subscribe-base core method signal)]
          (assert-spec method ::subscribe.output output)
          output)
        (catch js/Object e (println "error in subscribe unit: " e))))))

(defmethod ig/init-key ::subscribe
  [_ config]
  (create-subscribe-instance config))

;; ------------------------------------------------
;; component

(defn create-component-instance
  [config]
  (fn component [method & args]
    (let [core (assoc config :component component)]
      (apply component-base core method args))))

(defmethod ig/init-key ::component
  [_ config]
  (create-component-instance config))

;; ------------------------------------------------
;; view

(s/def ::view.method ::method)

(defn create-view-instance
  [config]
  (fn view [method props & args]
    (let [core (assoc config :view view)]
      (try
        (let [output (vec (concat [view-base core method props] args))]
          output)
        (catch js/Object e (println "error in view unit: " e))))))

(defmethod ig/init-key ::view
  [_ config] 
  (create-view-instance config))

;; ------------------------------------------------
;; chan

(defmethod ig/init-key ::chan
  [_ _]
  (chan))

;; ------------------------------------------------
;; dispatch 

(s/def ::dispatch.input ::request)

(defn create-dispatch-instance
  [config]
  (let [{:keys [out-chan]} config]
    (fn dispatch [& args]
      (go (>! out-chan args)))))

(defmethod ig/init-key ::dispatch
  [_ config]
  (create-dispatch-instance config))

;; ------------------------------------------------
;; inject

(defmethod inject-base :inject-all
  [{:keys [inject-keys] :as core} _ request]
  (reduce (fn [req k]
            (inject-base core k req))
          request inject-keys))

(defmethod inject-base :posh-db
  [{:keys [pconn]} _method req]
  (assoc-in req [:_env :db] @pconn))

(defmethod inject-base :ratom-db
  [{:keys [ratom]} _method req]
  (assoc-in req [:_env :ratom-db] @ratom))

(s/def ::inject.input (s/keys :opt-un [::_env]))
(s/def ::inject.output (s/keys :req-un [::_env]))

(defn create-inject-instance
  [config]
  (fn inject [method req]
    (let [core config]
      (try
        ;; (assert-spec method ::inject.input req)
        (let [output (inject-base core method req)]
          (assert-spec method ::inject.output output)
          output)
        (catch js/Object e (println (str e)))))))

(defmethod ig/init-key ::inject
  [_ config]
  (create-inject-instance config))

;; ------------------------------------------------
;; model

(defn create-model-instance
  [config]
  (fn model [method & args]
    (let [core (assoc config :model model)]
      (apply model-base core method args))))

(defmethod ig/init-key ::model
  [_ config]
  (create-model-instance config))

;; ------------------------------------------------
;; handle

(s/def ::handle.input (s/keys :req-un [::_env]))
(s/def ::handle.output (s/or :nil nil? :effect ::effect))

(defn create-handle-instance
  [config]
  (fn handle [method req]
    (let [core (assoc config :handle handle)]
      (try
        (let [output (handle-base core method req)]
          output)
        (catch js/Object e (println "error in handle unit: " method req e))))))

(defmethod ig/init-key ::handle
  [_ config]
  (create-handle-instance config))

;; ------------------------------------------------
;; do 

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
  (println "log-out: ")
  (pprint value))

(defmethod do-base :log-error
  [_ _ value]
  (println "log-error: ")
  (pprint value))

(defmethod do-base :tx
  [{:keys [pconn]} _ tx]
  (p/transact! pconn tx)
  tx)

(defmethod do-base :reset-ratom
  [{:keys [ratom]} _ value]
  (reset! ratom value))

(defmethod do-base :assoc-ratom
  [{:keys [ratom]} _ {:keys [path value]}]
  (let [cursor (r/cursor ratom path)]
    (reset! cursor value)))

(defmethod do-base :update-ratom
  [{:keys [ratom]} _ {:keys [path args]}]
  (let [cursor (r/cursor ratom path)]
    (apply swap! cursor args)))

(defmethod do-base :dissoc-ratom
  [{:keys [ratom]} _ {:keys [path]}]
  (let [cursor (r/cursor ratom path)]
    (reset! cursor nil)))

(defn create-do-instance
  [config]
  (fn do! [method & args]
    (let [core config]
      (apply do-base core method args))))

(defmethod ig/init-key ::do!
  [_ config]
  (create-do-instance config))

;; ------------------------------------------------
;; process

(defmethod process-base :default
  [{:keys [do! handle inject] :as core} method & args]
  ;; (println "in process default" method args)
  (let [handle-mathods (methods handle-base)]
    (if (contains? handle-mathods method)
      (let [req' (first args)
            req (inject :inject-all req')
            effect (handle method req)]
        (doseq [action effect]
          (if (contains? handle-mathods (first action))
            (apply process-base core action)
            (apply do! action))))
      (apply do! method args))))

(defn create-process-instance
  [config]
  (fn process [& args]
    (let [core config]
      (try
        (let [output (apply process-base core args)]
          output)
        (catch js/Object e (println "error in process: " e))))))

(defmethod ig/init-key ::process
  [_ config]
  (create-process-instance config))

;; ------------------------------------------------
;; service

(defn create-service-instance
  [config]
  (let [{:keys [process in-chan]} config]
    (go-loop []
      (let [action (<! in-chan)]
        ;; (println "in service: " action)
        (if (and (map? (second action)) (get-in (second action) [:_props :sync?]))
          (apply process action)
          (go (apply process action))))
      (recur))
    {:in-chan in-chan}))

(defmethod ig/init-key ::service
  [_ config]
  (create-service-instance config))

;; ------------------------------------------------
;; schedule

(defn create-schedule-instance
  [core]
  (fn schedule [method & args]
    (apply schedule-base core method args)))

(defmethod ig/init-key ::schedule
  [_ config]
  (create-schedule-instance config))
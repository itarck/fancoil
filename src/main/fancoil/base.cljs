(ns fancoil.base)


(defmulti spec-base
  (fn [core method & args] method))


(defmulti schema-base
  (fn [config signal & args] signal))

(defmulti subscribe-base
  "stateful function: subscribe a ratom or reaction
   reaction or ratom in -> reaction out
   core: db-ref
   "
  (fn [core method & args] method))


(defmulti view-base
  "stateful function: view a entity
   props in -> reagent views
   core: subscribe, dispatch
   "
  (fn [core method & args] method))

(defmulti inject-base
  "stateful function: inject a cofx
   request in -> request out
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmulti model-base
  "pure function: tap a model
   value in -> value out
   "
  (fn [core method & args] method))

(defmulti handle-base
  "pure function: handle a request
   request in -> response out
   {:db db} -> {:tx tx}"
  (fn [core method & args] method))

(defmulti do-base
  "stateful function: do a fx
   response in -> do effects
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmulti process-base
  "stateful function: process a request to fx
   request in -> effects
   core: ratom, other resources
   "
  (fn [core method & args] method))

(defmulti schedule-base
  "stateful function: schedule a task once or periodic
   task in -> event out
   core: dispatch
   "
  (fn [core method & args] method))

(defmulti component-base 
  "view component"
  (fn [core method & args] method))
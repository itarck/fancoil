(ns fancoil.base)


(defmulti model
  "pure function: tap a model
   value in -> value out
   "
  (fn [core method & args] method))

(defmulti handle
  "pure function: handle a request
   request in -> response out
   {:db db} -> {:tx tx}"
  (fn [core method & args] method))

(defmulti inject
  "stateful function: inject a cofx
   request in -> request out
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmulti do!
  "stateful function: do a fx
   response in -> do effects
   core: db-ref, other resources
   "
  (fn [core method & args] method))

(defmulti process
  "stateful function: process a request to fx
   request in -> effects
   core: ratom, other resources
   "
  (fn [core method & args] method))

(defmulti subscribe
  "stateful function: subscribe a ratom or reaction
   reaction or ratom in -> reaction out
   core: db-ref
   "
  (fn [core method & args] method))

(defmulti view
  "stateful function: view a entity
   props in -> reagent views
   core: subscribe, dispatch
   "
  (fn [core method & args] method))

(defmulti schedule
  "stateful function: schedule a task once or periodic
   task in -> event out
   core: dispatch
   "
  (fn [core method & args] method))

(defmulti spec
  (fn [core method & args] method))

(defmulti schema
  (fn [config signal & args] signal))


(defmulti system
  (fn [config method & args] method))



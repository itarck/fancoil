(ns fancoil.base)


;; new version

(defmulti tap
  "pure function: tap a model
   value in -> value out
   "
  (fn [config signal & rest] signal))

(defmulti handle
  "pure function: handle a request
   request in -> response out
   {:db db} -> {:tx tx}"
  (fn [config signal & rest] signal))

(defmulti inject 
  "stateful function: inject a cofx
   request in -> request out
   config: db-ref, other resources
   "
  (fn [config signal & rest] signal))

(defmulti do! 
  "stateful function: do a fx
   response in -> do effects
   config: db-ref, other resources
   "
  (fn [config signal & rest] signal))

(defmulti handle!
  "stateful function: process a request to fx
   request in -> effects
   config: ratom, other resources
   "
  (fn [config signal & rest] signal))

(defmulti subscribe 
  "stateful function: subscribe a ratom or reaction
   reaction or ratom in -> reaction out
   config: db-ref
   "
  (fn [config signal & rest] signal))

(defmulti view
  "stateful function: view a entity
   props in -> reagent views
   config: subscribe, dispatch
   "
  (fn [config signal & rest] signal))




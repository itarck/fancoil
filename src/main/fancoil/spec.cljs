(ns fancoil.spec
  (:require
   [cljs.spec.alpha :as s]
   [reagent.ratom :as ra]))


;; system, ctx

(s/def :system/core map?)
(s/def :system/ident keyword?)
(s/def :system/method keyword?)
(s/def :system/ctx some?)
(s/def :system/action (s/cat :method :system/method
                             :ctx :system/ctx))

(s/def :ctx/event map?)
(s/def :ctx/env map?)
(s/def :ctx/effect (s/coll-of :system/action))
(s/def :ctx/scope map?)
(s/def :ctx/signal map?)
(s/def :ctx/hiccup vector?)
(s/def :ctx/props map?)
(s/def :ctx/request (s/keys :req-un [:ctx/event :ctx/env]
                            :opt-un [:ctx/props]))
(s/def :ctx/reaction (fn [t] (= ra/Reaction (type t))))


;; unit leval

(s/def :subscribe/core map?)
(s/def :subscribe/method :system/method)
(s/def :subscribe/input :ctx/signal)
(s/def :subscribe/output :ctx/reaction)


(s/def :view/core map?)
(s/def :view/method :system/method)
(s/def :view/input :ctx/scope)
(s/def :view/output some?)


(s/def :dispatch/input :ctx/request)


(s/def :process/input (s/keys :req-un [:ctx/event]
                              :opt-un [:ctx/env]))

(s/def :inject/input (s/keys :req-un [:ctx/event] :opt-un [:ctx/env]))
(s/def :inject/output (s/keys :req-un [:ctx/event :ctx/env]))


(s/def :handle/input (s/keys :req-un [:ctx/event :ctx/env]))
(s/def :handle/output :ctx/effect)



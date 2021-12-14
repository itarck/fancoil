(ns fancoil.test-core
  (:require
   [fancoil.core :as fc]
   [integrant.core :as ig]))


(def config
  {::fc/ratom {}})


(ig/init config)
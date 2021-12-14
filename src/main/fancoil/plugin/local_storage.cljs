(ns fancoil.plugin.local-storage
  (:require
   [cljs.reader]
   [fancoil.base :as base]))


;; -- Local Storage  ----------------------------------------------------------
;;
;; Part of the todomvc challenge is to store todos in LocalStorage, and
;; on app startup, reload the todos from when the program was last run.
;; But the challenge stipulates to NOT load the setting for the "showing"
;; filter. Just the todos.
;;

(defmethod base/inject :local-storage/load-entity
  [env _fn {:keys [local-storage-key]} req]
  (let [entity (into (hash-map)
                     (some->> (.getItem js/localStorage local-storage-key)
                              (cljs.reader/read-string)))]
    (assoc-in req [:local-storage/entity local-storage-key] entity)))


(defmethod base/do! :local-storage/save-entity
  [env _fn {:keys [local-storage-key entity]}]
  (.setItem js/localStorage local-storage-key (str entity)))


(comment

  (def env {})

  (base/do! env :local-storage/save-entity {:local-storage-key "mykey"
                                            :entity {:hello "world"}})
  
  (base/inject env :local-storage/load-entity {:local-storage-key "mykey"} {})

  )
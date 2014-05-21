(ns service.notifier
  (:require [taoensso.carmine :as car :refer (wcar)]
            [carica.core :refer [config]])
  (:use org.httpkit.server))

(def pool (car/make-conn-pool))
(def spec-server1 (car/make-conn-spec))
(defmacro wcar* [& body] `(car/with-conn pool spec-server1 ~@body))

(defn api-handler [request]
  (with-channel request channel
    (def listener
      (car/with-new-pubsub-listener
       spec-server1 {(config :channel) (fn [data]
                                          (send! channel (pr-str data)))}
       (car/subscribe (config :channel))))
    (on-close channel (fn [status]
                        (car/close-listener listener)))))


(defn notify [mtype username id content]
  (wcar* (car/publish (config :channel)
                      (pr-str {:mtype mtype
                               :username username
                               :id id
                               :content content}))))

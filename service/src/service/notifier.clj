(ns service.notifier
  (:require [taoensso.carmine :as car :refer (wcar)]
            [carica.core :refer [config]])
  (:use org.httpkit.server))

(def pool (car/make-conn-pool))
(def spec-server1 (car/make-conn-spec :uri (config :redis-uri)))
(defmacro wcar* [& body] `(car/with-conn pool spec-server1 ~@body))

(defn- create-listener [channel]
  (car/with-new-pubsub-listener
    spec-server1 {(config :channel) (fn [data]
                                      (send! channel (pr-str data)))}
     (car/subscribe (config :channel))))

(defn api-handler [request]
  (with-channel request channel
    (let [listener (create-listener channel)]
      (on-close channel (fn [status]
                          (car/close-listener listener))))))

(defn notify [mtype id]
  (wcar* (car/publish (config :channel)
                      (pr-str {:mtype mtype
                               :id id}))))

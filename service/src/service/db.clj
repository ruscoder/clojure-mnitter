(ns service.db
  (:use carica.core
        korma.db
        korma.core))

(defdb db (postgres {:db (config :db :name)
                     :user (config :db :user)
                     :host (config :db :host)
                     :port (config :db :port)
                     :password (config :db :pass)}))

(defentity user)
(defentity note (belongs-to user))

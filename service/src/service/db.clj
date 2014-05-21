(ns service.db)

(ns service.db
  (:use carica.core
        korma.db
        korma.core))
(defdb db {:classname "com.mysql.jdbc.Driver"
           :subprotocol "mysql"
           :user (config :db :user)
           :password (config :db :pass)
           :subname (str "//127.0.0.1:3306/" (config :db :name) "?useUnicode=true&characterEncoding=utf8")
           :delimiters "`"})

(defentity user)
(defentity note (belongs-to user))

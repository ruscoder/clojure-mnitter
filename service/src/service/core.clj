(ns service.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io]
            [clj-time.core]
            [clj-time.coerce]
            [service.notifier :as notifier]
            [org.httpkit.server :as kit])
  (:use compojure.core
        compojure.handler
        ring.middleware.edn
        carica.core
        korma.db
        korma.core
        [digest :only [md5]]))

(defdb db {:classname "com.mysql.jdbc.Driver"
           :subprotocol "mysql"
           :user (config :db :user)
           :password (config :db :pass)
           :subname (str "//127.0.0.1:3306/" (config :db :name) "?useUnicode=true&characterEncoding=utf8")
           :delimiters "`"})

(defentity user)
(defentity note (belongs-to user))

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn- get-session-by-name [request name]
  (get (:session request) name))

(defn- set-session-by-name [request name value]
  (let [session (:session request)]
    (merge session {name value})))

(load "user")
(load "note")

(defroutes compojure-handler
  (GET "/" [] (slurp (io/resource "public/html/index.html")))
  (GET "/rpc" [] notifier/api-handler)
  (context "/user" [] user-routes)
  (context "/note" [] note-routes)
  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))

(defn -main [& args]
  (kit/run-server app {:port 8000}))

(ns service.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io]
            [clj-time.core]
            [clj-time.coerce]
            [org.httpkit.server :as kit])
  (:use compojure.core
        compojure.handler
        ring.middleware.edn
        carica.core
        [service.notifier :only [api-handler]]
        [service.user :only [user-routes]]
        [service.note :only [note-routes]]))

(defroutes compojure-handler
  (GET "/" [] (slurp (io/resource "public/html/index.html")))
  (GET "/rpc" [] api-handler)
  (context "/user" [] user-routes)
  (context "/note" [] note-routes)
  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))

(defn -main [port]
  (kit/run-server app {:port (Integer. port)}))

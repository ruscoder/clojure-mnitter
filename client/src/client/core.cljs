(ns client.core
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events]
            [client.note :refer [try-load-notes-for-user
                                 try-load-notes-all
                                 init-websocket]]
            [client.user :refer [try-load-userinfo]]
            [client.common :refer [current-username
                                   error-handler
                                   goog-history]])
  (:require-macros [enfocus.macros :as em])
  (:import goog.history.EventType))

(em/defsnippet mnitter-header "/html/base.html" "#header" [])
(em/defsnippet mnitter-sidebar "/html/base.html" "#sidebar" [])
(em/defsnippet mnitter-content "/html/base.html" "#content" [])

; Routing and enter point
(defn start []
  (ef/at ".container"
         (ef/do-> (ef/content (mnitter-header))
                  (ef/append (mnitter-content))
                  (ef/append (mnitter-sidebar))))
  (try-load-userinfo)
  (init-websocket (str "ws://" (.-host js/location) "/rpc")))

(defroute "/" [] nil
  (try-load-notes-all))

(defroute "/:user" [user]
  (try-load-notes-for-user user))

(set! (.-onload js/window) #(em/wait-for-load (start)))

(doto goog-history
  (goog.events/listen EventType/NAVIGATE
                      #(em/wait-for-load (secretary/dispatch! (.-token %))))
  (.setEnabled true))

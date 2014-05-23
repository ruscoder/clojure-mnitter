(ns client.common
  (:import goog.History))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "Something bad happened: " status " " status-text)))

(def current-username (atom nil))
(set! current-username nil)

(def goog-history (History.))

(defn get-current-location-user []
  (.substring (.getToken goog-history) 1))

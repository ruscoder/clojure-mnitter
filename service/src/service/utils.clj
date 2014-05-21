(ns service.utils)

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-session-by-name [request name]
  (get (:session request) name))

(defn set-session-by-name [request name value]
  (let [session (:session request)]
    (merge session {name value})))

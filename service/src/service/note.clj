(defn- sql-datetime-now []
  (clj-time.coerce/to-sql-time (clj-time.core/now)))

(defn- note-get [id]
  (first (select note (where {:id id}))))

(defn- note-is-my? [request id]
  (let [user-id (get-session-by-name request :user)
        note (note-get id)]
    (and user-id (= (:user_id note) user-id))))

(defn note-list-all []
  (response (select note (with user (fields :username))
                                    (order :date :DESC))))

(defn note-list-for-user [username]
  (response (select note (with user (fields :username)
                                    (where {:username username}))
                                    (order :date :DESC))))


(defn note-create [request content]
  (let [user-id (get-session-by-name request :user)]
    (if-not user-id
      (response nil 401)
      (response (insert note (values {:user_id user-id
                                      :content content
                                      :date (sql-datetime-now)}))))))

(defn note-update [request id content]
  (if-not (note-is-my? request id)
    (response nil 403)
    (response (update note
                      (set-fields {:content content})
                      (where {:id id})))))

(defn note-delete [request id]
    (if-not (note-is-my? request id)
      (response nil 403)
      (response (delete note
                        (where {:id id})))))

(defroutes note-routes
  (POST "/delete/:id" [id :as req] (note-delete req id))
  (POST "/update/:id" [id content :as req] (note-update req id content))
  (GET "/list-all" [] (note-list-all))
  (GET "/list-user" [username] (note-list-for-user username))
  (POST "/create" [content :as req] (note-create req content)))

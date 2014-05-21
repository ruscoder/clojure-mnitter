(defn- sql-datetime-now []
  (clj-time.coerce/to-sql-time (clj-time.core/now)))

(defn- note-get [id]
  (first (select note (where {:id id}))))

(defn- note-is-my? [request id]
  (let [user-id (get-session-by-name request :user)
        note (note-get id)]
    (and user-id (= (:user_id note) user-id))))

(defn note-list-all-view []
  (response (select note (with user (fields :username))
                                    (order :date :DESC))))

(defn note-list-for-user-view [username]
  (response (select note (with user (fields :username)
                                    (where {:username username}))
                                    (order :date :DESC))))

(defn- note-create [user-id content]
  (let [note (insert note (values {:user_id user-id
                                   :content content
                                   :date (sql-datetime-now)}))]
    (when note
      (notifier/notify "create" user-id (:GENERATED_KEY note) content))))

(defn- note-update [id content]
  (update note
    (set-fields {:content content})
    (where {:id id}))
  (notifier/notify "update" nil id content))

(defn- note-delete [id]
  (delete note (where {:id id}))
  (notifier/notify "delete" nil id nil))

(defn note-create-view [request content]
  (let [user-id (get-session-by-name request :user)]
    (if-not user-id
      (response nil 401)
      (response (note-create user-id content)))))

(defn note-update-view [request id content]
  (if-not (note-is-my? request id)
    (response nil 403)
    (response (note-update id content))))

(defn note-delete-view [request id]
    (if-not (note-is-my? request id)
      (response nil 403)
      (response (note-delete id))))

(defroutes note-routes
  (POST "/delete/:id" [id :as req] (note-delete-view req id))
  (POST "/update/:id" [id content :as req] (note-update-view req id content))
  (GET "/list-all" [] (note-list-all-view))
  (GET "/list-user" [username] (note-list-for-user-view username))
  (POST "/create" [content :as req] (note-create-view req content)))

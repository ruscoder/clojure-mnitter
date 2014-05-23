(ns service.note
  (:require clj-time.core
            clj-time.coerce)
  (:use [service.db :only [note user]]
        [service.notifier :only [notify]]
        service.utils
        korma.core
        [compojure.core :only [defroutes POST GET]]))

(defn- sql-datetime-now []
  (clj-time.coerce/to-sql-time (clj-time.core/now)))

(defn- note-get [id]
  (first (select note (with user
                        (fields :username))
                      (where {:id (parse-int id)}))))

(defn- note-is-my? [request id]
  (let [user-id (get-session-by-name request :user)
        note (note-get id)]
    (and user-id (= (:user_id note) user-id))))

(defn note-list-all-view [user-offset]
  (response (select note (with user (fields :username))
                                    (order :date :DESC)
                                    (offset user-offset)
                                    (limit 10))))

(defn note-list-for-user-view [username user-offset]
  (response (select note (with user (fields :username)
                                    (where {:username username}))
                                    (order :date :DESC)
                                    (offset user-offset)
                                    (limit 10))))

(defn- note-create [user-id content]
  (let [note (insert note (values {:user_id user-id
                                   :content content
                                   :date (sql-datetime-now)}))]
    (when note
      (notify "create" (:id note))
      note)))

(defn- note-update [id content]
  (update note
    (set-fields {:content content})
    (where {:id (parse-int id)}))
  (notify "update" id))

(defn- note-delete [id]
  (delete note (where {:id (parse-int id)}))
  (notify "delete" id))

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

(defn note-one-view [id]
  (response (note-get id)))

(defroutes note-routes
  (POST "/delete/:id" [id :as req] (note-delete-view req id))
  (POST "/update/:id" [id content :as req] (note-update-view req id content))
  (GET "/list-all" {{offset :offset} :params} (note-list-all-view offset))
  (GET "/list-user/:username" [username :as {{offset :offset} :params}] (note-list-for-user-view username offset))
  (POST "/create" [content :as req] (note-create-view req content))
  (GET "/:id" [id] (note-one-view id)))

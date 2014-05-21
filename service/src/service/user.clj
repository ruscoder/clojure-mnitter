(defn- user-find [username email]
   (first (select user (where (or (= :username username)
                                  (= :email email))))))

(defn- user-get [username password]
  (first (select user (where {:username username
                              :password (md5 password)}))))

(defn user-create-view [username email password]
  (if (or (user-find username email)
          (not (insert user (values {:username username
                                     :email email
                                     :password (md5 password)}))))
    (response nil 420)
    (response nil)))

(defn user-current-view [request]
  (let [user-id (get-session-by-name request :user)
        token (md5 (str (config :secret-key) user-id))]
    (if-not user-id
      (response nil 401)
      (response {:token token
                 :user-id user-id}))))

(defn user-auth-view [request username password]
  (let [user-entry (user-get username password)
        user-id (:id user-entry)
        updated-session (set-session-by-name request :user user-id)]
    (-> (if-not user-entry
          (response nil 401)
          (response {:user user-id}))
        (assoc :session updated-session))))

(defn user-logout-view [request]
  (user-auth-view request nil nil))

(defroutes user-routes
  (POST "/create" [username email password] (user-create-view username email password))
  (POST "/auth" [username password :as req] (user-auth-view req username password))
  (GET "/logout" req (user-logout-view req))
  (GET "/" req (user-current-view req)))

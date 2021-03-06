(ns service.user
  (:use [service.db :only [user]]
        service.utils
        korma.core
        [compojure.core :only [defroutes POST GET]]
        [digest :only [md5]]))

(defn- user-find [username email]
   (first (select user (where (or (= :username username)
                                  (= :email email))))))

(defn- user-get [username password]
  (let [user (first (select user (where {:username username
                                         :password (md5 password)})))]
    (dissoc user :password)))

(defn- user-get-by-id [user-id]
  (first (select user (where {:id user-id}))))

(defn user-create-view [username email password]
  (if (or (user-find username email)
          (not (insert user (values {:username username
                                     :email email
                                     :password (md5 password)}))))
    (response nil 420)
    (response nil)))

(defn user-current-view [request]
  (let [user-id (get-session-by-name request :user)
        user (user-get-by-id user-id)]
    (if-not user-id
      (response nil 401)
      (response user))))

(defn user-auth-view [request username password]
  (let [user-entry (user-get username password)
        user-id (:id user-entry)
        updated-session (set-session-by-name request :user user-id)]
    (-> (if-not user-entry
          (response nil 401)
          (response {:user user-id}))
        (assoc :session updated-session))))

(defroutes user-routes
  (POST "/create" [username email password] (user-create-view username email password))
  (POST "/auth" [username password :as req] (user-auth-view req username password))
  (GET "/logout" req (user-auth-view req nil nil))
  (GET "/" req (user-current-view req)))

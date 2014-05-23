(ns client.user
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary]
            [client.common :refer [current-username]]
            [client.note :refer [note-add-button]])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet reg-form "/html/base.html" "#reg-form" [])
(em/defsnippet login-form "/html/base.html" "#login-form" [])
(em/defsnippet userinfo-form "/html/base.html" "#userinfo-form" [username]
               "#username" (ef/content username))

(defn ^:export show-reg-form []
  (ef/at "#sidebar" (ef/content (reg-form))))

(defn ^:export show-login-form []
  (ef/at "#sidebar" (ef/content (login-form)))
  (ef/at "#note-form-container" (ef/content "")))

(defn userinfo-success [data]
  ; Callback handler
  (let [username (:username data)]
    (set! current-username username)
    (ef/at "#sidebar"
      (ef/content (userinfo-form username))))
    (ef/at "#note-form-container" (ef/content (note-add-button))))

(defn userinfo-failed [status status-text]
  ; Callback handler
  (set! current-username "anonymous")
  (show-login-form))

(defn try-load-userinfo []
  (GET "/user/"
       {:handler userinfo-success
        :error-handler userinfo-failed}))

(defn user-login-success []
  ; Callback handler
  (try-load-userinfo)
  (secretary/dispatch! "/"))

(defn user-login-failed []
  ; Callback handler
  (js/alert "Not authenticated: invalid login or password."))

(defn ^:export user-login []
  (POST "/user/auth"
       {:params {:username (ef/from "#user-username" (ef/read-form-input))
                 :password (ef/from "#user-password" (ef/read-form-input))}
        :handler user-login-success
        :error-handler user-login-failed}))

(defn user-logout-success []
  ; Callback handler
  (try-load-userinfo)
  (secretary/dispatch! "/"))

(defn ^:export user-logout []
  (GET "/user/logout"
       {:error-handler user-logout-success}))

(defn user-reg-success [data]
  ; Callback handler
  (js/alert "Registration complete. Use login form for login!")
  (show-login-form))

(defn user-reg-failed [status status-text]
  ; Callback handler
  (js/alert "User with this username or email is exists!"))

(defn ^:export user-reg []
  (let [username (ef/from "#user-username" (ef/read-form-input))
        email (ef/from "#user-email" (ef/read-form-input))
        password (ef/from "#user-password" (ef/read-form-input))
        password-retry (ef/from "#user-password-retry" (ef/read-form-input))]
    (if (not= password password-retry)
      (js/alert "Password's is not equal!")
      (POST "/user/create"
           {:params {:username username
                     :email email
                     :password password}
            :handler user-reg-success
            :error-handler user-reg-failed}))))

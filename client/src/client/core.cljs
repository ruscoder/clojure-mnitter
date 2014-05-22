(ns client.core
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events])
  (:require-macros [enfocus.macros :as em])
  (:import goog.History
           goog.history.EventType))

(em/defsnippet blog-header "/html/base.html" ".blog-header" [])
(em/defsnippet blog-sidebar "/html/base.html" "#sidebar" [])
(em/defsnippet blog-content "/html/base.html" "#content" [])
(em/defsnippet blog-create-post "/html/base.html" "#article-form" [])
(em/defsnippet reg-form "/html/base.html" "#reg-form" [])
(em/defsnippet login-form "/html/base.html" "#login-form" [])
(em/defsnippet userinfo-form "/html/base.html" "#userinfo-form" [username]
               "#username" (ef/content username))

(em/defsnippet blog-edit-post "/html/base.html" "#article-form"
  [{:keys [id title body]}]
  "#article-title" (ef/set-attr :value title)
  "#article-body" (ef/content body)
  "#save-btn" (ef/set-attr :onclick (str "client.core.try_update_article(" id ")")))

(em/defsnippet blog-post-view "/html/base.html" "#view-article" [{:keys [title body]}]
  "#view-title" (ef/content title)
  "#view-body" (ef/content body))

(em/defsnippet blog-post "/html/base.html"
  ".blog-post"  [{:keys [id title body]}]
  "#blog-post-title" (ef/content title)
  "#blog-post-body" (ef/content body)
  "#article-view" (ef/set-attr :onclick
                    (str "client.core.try_view_article(" id ")"))
  "#article-edit" (ef/set-attr :onclick
                    (str "client.core.try_edit_article(" id ")"))
  "#article-delete" (ef/set-attr :onclick
  (str "if(confirm('Really delete?')) client.core.try_delete_article(" id ")")))


(defn ^:export show-reg-form []
  (ef/at "#sidebar" (ef/content (reg-form))))

(defn ^:export show-login-form []
  (ef/at "#sidebar" (ef/content (login-form))))

(defn userinfo-success [data]
  (ef/at "#sidebar"
    (ef/content (userinfo-form (:username data)))))

(defn userinfo-failed [status status-text]
  (show-login-form))

(defn try-load-userinfo []
  (GET "/user/"
       {:handler userinfo-success
        :error-handler userinfo-failed}))

(defn start []
  (ef/at ".container"
         (ef/do-> (ef/content (blog-header))
                  (ef/append (blog-content))
                  (ef/append (blog-sidebar))))
  (try-load-userinfo))

(defn hide-new-post-btn []
  (ef/at "#new-post" (ef/set-attr :style "display:none;")))

(defn ^:export show-create []
  (ef/at "#inner-content" (ef/content (blog-create-post)))
  (hide-new-post-btn))

(defn ^:export close-form []
  (start))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "Something bad happened: " status " " status-text)))

(defn article-saved [response]
  (close-form))

(defn user-login-success []
  (try-load-userinfo))

(defn user-logout-success []
  (start))

(defn user-login-failed []
  (js/alert "Not authenticated: invalid login or password."))

(defn ^:export user-login []
  (POST "/user/auth"
       {:params {:username (ef/from "#user-username" (ef/read-form-input))
                 :password (ef/from "#user-password" (ef/read-form-input))}
        :handler user-login-success
        :error-handler user-login-failed}))

(defn ^:export user-logout []
  (GET "/user/logout"
       {:error-handler user-logout-success}))


(defn user-reg-success [data]
  (js/alert "Registration complete. Use login form for login!")
  (show-login-form))

(defn user-reg-failed [status status-text]
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

(defn ^:export try-update-article [id]
  (POST (str "/article/update/" id)
        {:params {:title (ef/from "#article-title" (ef/read-form-input))
                  :body  (ef/from "#article-body" (ef/read-form-input))}
         :handler article-saved
         :error-handler error-handler}))

(defn ^:export article-edit [data]
  (ef/at "#inner-content" (ef/content (blog-edit-post data)))
  (hide-new-post-btn))

(defn ^:export try-edit-article [id]
  (GET (str "/article/" id)
        {:handler article-edit
         :error-handler error-handler}))

(defn article-view [data]
  (hide-new-post-btn)
  (ef/at "#inner-content" (ef/content (blog-post-view data))))

(defn ^:export try-view-article [id]
  (GET (str "/article/" id)
        {:handler article-view
         :error-handler error-handler}))

(defn ^:export try-delete-article [id]
  (POST (str "/article/delete/" id)
        {:handler article-saved
         :error-handler error-handler}))

(defn ^:export try-create-article []
  (.log js/console (ef/from "#article-title" (ef/read-form-input)))
  (.log js/console (ef/from "#article-body" (ef/read-form-input)))
  (POST "/article/create"
        {:params {:title (ef/from "#article-title" (ef/read-form-input))
                  :body (ef/from "#article-body" (ef/read-form-input))}
         :handler article-saved
         :error-handler error-handler}))

(defroute "/" [] nil)
  ;(ef/at "#login-form" (ef/content "FUCK root route")))

(defroute "/:user" []
  (ef/at "#login-form" (ef/content "FUCK ROUTE")))


(def goog-history (History.))

(set! (.-onload js/window) #(em/wait-for-load (start)
                                              (secretary/dispatch! (.getToken goog-history))))

(doto goog-history
  (goog.events/listen EventType/NAVIGATE
                      #(em/wait-for-load (secretary/dispatch! (.-token %))))
  (.setEnabled true))

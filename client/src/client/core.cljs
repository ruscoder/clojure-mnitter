(ns client.core
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]]
            [cljs.reader :as reader]
            [secretary.core :as secretary :include-macros true :refer [defroute]]
            [goog.events :as events])
  (:require-macros [enfocus.macros :as em])
  (:import goog.History
           goog.history.EventType))

(em/defsnippet mnitter-header "/html/base.html" "#header" [])
(em/defsnippet mnitter-sidebar "/html/base.html" "#sidebar" [])
(em/defsnippet mnitter-content "/html/base.html" "#content" [])
(em/defsnippet note-create-form "/html/base.html" "#note-form" [])
(em/defsnippet reg-form "/html/base.html" "#reg-form" [])
(em/defsnippet login-form "/html/base.html" "#login-form" [])
(em/defsnippet userinfo-form "/html/base.html" "#userinfo-form" [username]
               "#username" (ef/content username))
(em/defsnippet note-form-container "/html/base.html" "#note-form-container" [])
(em/defsnippet note-add-button "/html/base.html" "#note-add-button" [])

(def current-username (atom nil))
(def notes-count (atom 0))
(def goog-history (History.))
(set! current-username nil)

(em/defsnippet note-edit-form "/html/base.html" "#note-form" [id content]
  "#note-content" (ef/content content)
  "#save-btn" (ef/set-attr :onclick (str "client.core.try_update_note(" id ")"))
  "#cancel-btn" (ef/set-attr :onclick (str "client.core.show_note_by_id(" id ")")))

(em/defsnippet note-post "/html/base.html" "#note-post"  [{:keys [id username date content]}]
  "#note-post" (ef/add-class (str "note-post-" id))
  "#content" (ef/content content)
  "#date" (ef/content (.toLocaleString date))
  "#username" (ef/content username)
  "#controls" (if (= current-username username)
                (ef/remove-class "hidden")
                (ef/add-class "hidden"))
  "#note-edit" (ef/set-attr :onclick
                 (str "client.core.show_edit_note_form(" id ")"))
  "#note-delete" (ef/set-attr :onclick
                   (str "if(confirm('Really delete?')) client.core.try_delete_note(" id ")"))
  "#username-link" (ef/set-attr :href (str "/#/" username)))

(defn get-current-location-user []
  (.substring (.getToken goog-history) 1))

(defn ^:export show-reg-form []
  (ef/at "#sidebar" (ef/content (reg-form))))

(defn ^:export show-login-form []
  (ef/at "#sidebar" (ef/content (login-form)))
  (ef/at "#note-form-container" (ef/content "")))

(defn userinfo-success [data]
  (let [username (:username data)]
    (set! current-username username)
    (ef/at "#sidebar"
      (ef/content (userinfo-form username))))
    (ef/at "#note-form-container" (ef/content (note-add-button))))

(defn userinfo-failed [status status-text]
  (set! current-username "anonymous")
  (show-login-form))

(defn try-load-userinfo []
  (GET "/user/"
       {:handler userinfo-success
        :error-handler userinfo-failed}))

(defn hide-new-post-btn []
  (ef/at "#new-post" (ef/add-class "hidden")))

(defn ^:export show-create-note-form []
  (ef/at "#note-form-container" (ef/content (note-create-form)))
  (hide-new-post-btn))

(defn note-loaded [data]
  (let [selector (str ".note-post-" (:id data))]
    (ef/at selector (ef/content (note-post data)))))

(defn note-loaded-new [data]
  (let [username (:username data)
        location-user (get-current-location-user)]
    (when (or (= location-user username)
              (empty? location-user))
      (set! notes-count (inc notes-count))
      (ef/at "#inner-content" (ef/prepend (note-post data))))))

(defn ^:export show-note-by-id [id]
  (GET (str "/note/" id)
       {:handler note-loaded}))

(defn ^:export add-note-by-id [id]
  (GET (str "/note/" id)
       {:handler note-loaded-new}))

(defn ^:export remove-note-by-id [id]
  (ef/at (str ".note-post-" id)
         (ef/remove-node)))


(defn ^:export show-edit-note-form [id]
  (let [selector (str ".note-post-" id)
        content (ef/from (str selector " " "#content") (ef/get-text))]
    (ef/at selector (ef/content (note-edit-form id content)))))

(defn ^:export close-form []
  (ef/at "#note-form-container" (ef/content (note-add-button))))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "Something bad happened: " status " " status-text)))

(defn note-saved [response]
  (close-form))

(defn user-login-success []
  (try-load-userinfo)
  (secretary/dispatch! "/"))

(defn user-logout-success []
  (try-load-userinfo)
  (secretary/dispatch! "/"))

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

(defn ^:export try-update-note [id]
  (POST (str "/note/update/" id)
        {:params {:content  (ef/from "#note-content" (ef/read-form-input))}
         :handler note-saved
         :error-handler error-handler}))

(defn ^:export try-delete-note [id]
  (POST (str "/note/delete/" id)
        {:error-handler error-handler}))

(defn ^:export try-create-note []
  (let [content (.trim (ef/from "#note-content" (ef/read-form-input)))
        len (.-length content)]
  (if (zero? len)
    (js/alert "Please, write content!")
    (POST "/note/create"
          {:params {:content content}
           :handler note-saved
           :error-handler error-handler}))))

(defn note-list [data]
  (set! notes-count (count data))
  (ef/at "#inner-content" (ef/content (map note-post data))))

(defn try-load-notes [path]
  (if current-username
    (GET (str path)
          {:handler note-list
           :error-handler error-handler})
    (js/setTimeout #(try-load-notes path) 10)))

(defn more-note-list [data]
  (set! notes-count (+ notes-count (count data)))
  (ef/at "#inner-content" (ef/append (map note-post data))))

(defn try-load-more-notes [path]
    (GET (str path)
          {:params {:offset notes-count}
           :handler more-note-list
           :error-handler error-handler}))

(defn ^:export load-more-notes []
  (let [location-user (get-current-location-user)]
    (if (empty? location-user)
      (try-load-more-notes "/note/list-all")
      (try-load-more-notes (str "/note/list-user/" location-user)))))

(defn try-load-notes-for-user [user]
  (try-load-notes (str "/note/list-user/" user)))

(defn try-load-notes-all []
  (try-load-notes "/note/list-all"))

(defroute "/" [] nil
  (try-load-notes-all))

(defroute "/:user" [user]
  (try-load-notes-for-user user))

(defn ws-message-received [{:keys [mtype id]}]
  (case mtype
    "update" (show-note-by-id id)
    "create" (add-note-by-id id)
    "delete" (remove-note-by-id id)))

(defn ws-data-received [raw-data]
  (let [[command channel data] (reader/read-string raw-data)]
    (when (= command "message")
      (ws-message-received (reader/read-string data)))))

(defn init-websocket [url]
  (let [ws (js/WebSocket. url)]
    (set! (.-onmessage ws) #(ws-data-received (.-data %)))))

(defn start []
  (ef/at ".container"
         (ef/do-> (ef/content (mnitter-header))
                  (ef/append (mnitter-content))
                  (ef/append (mnitter-sidebar))))
  (try-load-userinfo)
  (init-websocket (str "ws://" (.-host js/location) "/rpc")))

(set! (.-onload js/window) #(em/wait-for-load (start)))

(doto goog-history
  (goog.events/listen EventType/NAVIGATE
                      #(em/wait-for-load (secretary/dispatch! (.-token %))))
  (.setEnabled true))

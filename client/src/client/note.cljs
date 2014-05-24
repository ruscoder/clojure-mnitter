(ns client.note
  (:require [enfocus.core :as ef]
            [ajax.core :refer [GET POST]]
            [cljs.reader :as reader]
            [client.common :refer [current-username
                                   get-current-location-user
                                   error-handler]])
  (:require-macros [enfocus.macros :as em]))

(em/defsnippet note-create-form "/html/base.html" "#note-form" [])
(em/defsnippet note-form-container "/html/base.html" "#note-form-container" [])
(em/defsnippet note-add-button "/html/base.html" "#note-add-button" [])


(em/defsnippet note-edit-form "/html/base.html" "#note-form" [id content]
  "#note-content" (ef/content content)
  "#save-btn" (ef/set-attr :onclick (str "client.note.try_update_note(" id ")"))
  "#cancel-btn" (ef/set-attr :onclick (str "client.note.show_note_by_id(" id ")")))

(em/defsnippet note-post "/html/base.html" "#note-post"  [{:keys [id username date content]}]
  "#note-post" (ef/add-class (str "note-post-" id))
  "#content" (ef/content content)
  "#date" (ef/content (.toLocaleString date))
  "#username" (ef/content username)
  "#controls" (if (= current-username username)
                (ef/remove-class "hidden")
                (ef/add-class "hidden"))
  "#note-edit" (ef/set-attr :onclick
                 (str "client.note.show_edit_note_form(" id ")"))
  "#note-delete" (ef/set-attr :onclick
                   (str "if(confirm('Really delete?')) client.note.try_delete_note(" id ")"))
  "#username-link" (ef/set-attr :href (str "/#/" username)))

(def notes-count (atom 0))

(defn hide-new-note-btn []
  (ef/at "#new-post" (ef/add-class "hidden")))

(defn ^:export show-create-note-form []
  (ef/at "#note-form-container" (ef/content (note-create-form)))
  (hide-new-note-btn))

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

(defn note-saved [response]
  (close-form))

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
  ; Callback handler
  (set! notes-count (count data))
  (ef/at "#inner-content" (ef/content (map note-post data))))

(defn try-load-notes [path]
  (if current-username
    (GET (str path)
          {:handler note-list
           :error-handler error-handler})
    (js/setTimeout #(try-load-notes path) 10)))

(defn more-note-list [data]
  ; Callback handler
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

; Websockets
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
  (let [ws (js/WebSocket. url)
        closed-connection (fn [] (js/console.log "closed. reconnecting")
                                 (js/setTimeout #(init-websocket url) 3000))]
    (set! (.-onmessage ws) #(ws-data-received (.-data %)))
    (set! (.-onclose ws) closed-connection)))

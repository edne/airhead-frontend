(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}
                  :upload-status ""
                  :playlist []
                  :library []}))

;; -------------------------
;;

(defn info-get []
  (GET "/api/info"
       {:handler #(swap! state assoc :info %)
        :response-format :json}))

(defn playlist-get []
  (GET "/api/playlist"
       {:handler #(swap! state assoc :playlist %)
        :response-format :json}))

(defn playlist-add [id]
  (PUT (str "/api/playlist/" id)))

(defn playlist-remove [id]
  (DELETE (str "/api/playlist/" id)))

(defn library-get []
  (GET "/api/library"
       {:handler #(swap! state assoc :library (% "tracks"))
        :response-format :json}))

(defn library-upload []
  ;; TODO: do not use element id
  (let [form (.getElementById js/document
                              "upload-form")
        on-success (fn []
                     (swap! state assoc :upload-status "Done!")
                     (.reset form))
        on-error #(swap! state assoc :upload-status "Something went wrong")]
    (swap! state assoc :upload-status "Uploading...")
    (POST "/api/library" {:enc-type "multipart/form-data"
                          :body (js/FormData. form)
                          :handler on-success
                          :error-handler on-error})))

(defn polling-callback []
  (info-get)
  (playlist-get)
  (library-get))

(js/setInterval polling-callback 1000)

;; -------------------------
;; Views

(defn info-component []
  (fn []
    (let [title   (get-in @state [:info "name"])
          message (get-in @state [:info "greet_message"])
          url     (get-in @state [:info "stream_url"])]
      [:div.info
       [:h1 title]
       [:div.greet-message message]
       [:div.player [:audio {:src url
                             :controls "controls"}]]])))

(defn upload-component []
  [:div.upload
   [:h2 "Upload"]
   [:form {:id "upload-form"}
    [:input {:type "file" :name "track"}]
    [:input {:type "button" :value "Upload" :on-click library-upload}]]
   [:div.upload-status (@state :upload-status)]])

(defn playlist-add-component [uuid]
  [:input.add-button
   {:type "button" :value "+"
    :on-click #(playlist-add uuid)}])

(defn playlist-remove-component [uuid]
  [:input.add-button
   {:type "button" :value "-"
    :on-click #(playlist-remove uuid)}])

(defn track-component [uuid]
  [:span.track
   ;(str " " (track "artist") " - " (track "title"))
   (str uuid)])

(defn now-playing-component []
  [:div.now-playing
   [:span "Now playing:"]
   [track-component (get-in @state [:playlist "current"])]])

(defn next-component []
  [:div.next
   [:span "Next:"]
   [:ul
    (for [uuid (get-in @state [:playlist "next"])]
      [:li
       [playlist-remove-component uuid]
       [track-component uuid]])]])

(defn playlist-component []
  [:div.library
   [:h2 "Playlist"]
   [now-playing-component]
   [next-component]])

(defn library-component []
  [:div.library
   [:h2 "Library"]
   [:ul (for [uuid (@state :library)]
          [:li
           [playlist-add-component uuid]
           [track-component uuid]])]])

(defn page-component []
  [:div.page
   [info-component]
   [upload-component]
   [playlist-component]
   [library-component]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

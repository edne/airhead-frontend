(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}
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
       {:handler #(swap! state assoc :playlist (% "next"))
        :response-format :json}))

(defn library-get []
  (GET "/api/library"
       {:handler #(swap! state assoc :library (% "tracks"))
        :response-format :json}))

(defn playlist-add [id]
  (PUT (str "/api/playlist/" id)))

(defn playlist-remove [id]
  (DELETE (str "/api/playlist/" id)))

;; -------------------------
;; Views

(defn info-component []
  (let [title   (get-in @state [:info "name"])
        message (get-in @state [:info "greet_message"])
        url     (get-in @state [:info "stream_url"])]
    [:div.info
     [:h1 title]
     [:div.greet-message message]
     [:div.player [:audio {:src url
                           :controls "controls"}]]]))

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
   (str uuid)
   ])

(defn playlist-component []
  (let [tracks (@state :playlist)]
    [:div.library
     [:h2 "Playlist"]
     [:ul (for [uuid tracks]
            [:li
             [playlist-remove-component uuid]
             [track-component uuid]])]]))

(defn library-component []
  (let [tracks (@state :library)]
    [:div.library
     [:h2 "Library"]
     [:ul (for [uuid tracks]
            [:li
             [playlist-add-component uuid]
             [track-component uuid]])]]))

(defn page-component []
  [:div.page
   [info-component]
   [playlist-component]
   [library-component]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (info-get)
  (playlist-get)
  (library-get)

  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}
                  :library {}}))

;; -------------------------
;;

(defn info-get []
  (GET "/api/info"
       {:handler #(swap! state assoc :info %)
        :response-format :json}))

(defn library-get []
  (GET "/api/library"
       {:handler #(swap! state assoc :library %)
        :response-format :json}))

(defn playlist-add [id]
  (PUT (str "/api/playlist/" id)))

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

(defn playlist-add-component [id]
  [:input.add-button
   {:type "button" :value "+"
    :on-click #(playlist-add id)}])

(defn track-component [track]
  [:span.track
   (str " " (track "artist") " - " (track "title"))])

(defn library-component []
  (let [tracks (-> @state
                   (get-in [:library "tracks"])
                   seq)]
    [:div.library
     [:h2 "Library"]
     [:ul (for [[id track] tracks]
            [:li
             [playlist-add-component id]
             [track-component track]])]]))

(defn page-component []
  [:div.page
   [info-component]
   [library-component]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (info-get)
  (library-get)

  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

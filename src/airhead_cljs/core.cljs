(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(def state (r/atom {:server "http://localhost:8080"
                    :playlist []
                    :tracks []}))

(defn update-state! [k value]
  (swap! state assoc k value))

(defn get-playlist []
  (GET "http://localhost:8080/api/queue"
       {:handler #(update-state! :playlist (% "items"))
        :response-format :json}))

(defn get-tracks []
  (GET "http://localhost:8080/api/tracks"
       {:handler #(update-state! :tracks (% "items"))
        :response-format :json}))

;; -------------------------
;; Views

(defn server-form []
  [:form
   [:input {:type "text"
            :value (@state :server)
            :on-change #(update-state! :server
                                       (-> % .-target .-value))}]])

(defn track-span [track]
  (str (track "artist") " - " (track "title")))

(defn playlist-div []
  (js/setTimeout get-playlist 1000)
  [:div [:h2 "Playlist"]
   [:ul (for [track (@state :playlist)]
          [:li (track-span track)])]])

(defn tracks-div []
  (js/setTimeout get-tracks 1000)
  [:div [:h2 "Tracks"]
   [:ul (for [track (@state :tracks)]
          [:li (track-span track)])]])

(defn home-page []
  [:div [:h1 "Airhead"]
   [server-form]
   [playlist-div]
   [tracks-div]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

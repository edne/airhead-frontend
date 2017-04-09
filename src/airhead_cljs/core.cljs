(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET PUT]]))

(def state (r/atom {:server "http://localhost:8080"
                    :playlist []
                    :tracks []}))

(defn update-state! [k value]
  (swap! state assoc k value))

(defn get-playlist []
  (GET (str (@state :server) "/api/queue")
       {:handler #(update-state! :playlist (% "items"))
        :response-format :json}))

(defn get-tracks []
  (GET (str (@state :server) "/api/tracks")
       {:handler #(update-state! :tracks (% "items"))
        :response-format :json}))

(defn enqueue-track [track]
  (let [uuid (track "uuid")]
    (PUT (str (@state :server) "/api/enqueue/" uuid))))

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

(defn enqueue-button [track]
  [:input {:type "button" :value "Enqueue"
           :on-click #(enqueue-track track)}])

(defn tracks-div []
  (js/setTimeout get-tracks 1000)
  [:div [:h2 "Tracks"]
   [:ul (for [track (@state :tracks)]
          [:li
           (enqueue-button track)
           (track-span track)])]])

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

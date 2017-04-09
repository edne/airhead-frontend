(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET PUT]]))

(defn get-json [url handler params]
  (GET url {:handler handler
            :params params
            :response-format :json}))

(defn enqueue-track [track]
  (let [uuid (track "uuid")]
    (PUT (str "/api/queue/" uuid))))

;; -------------------------
;; Views

(defn track-span [track]
  [:span (str (track "artist") " - " (track "title"))])

(defn playlist-section []
  (let [playlist (r/atom [])
        reset #(reset! playlist (% "items"))
        get-playlist #(get-json "/api/queue" reset {})]
    (fn []
      (js/setTimeout get-playlist 1000)
      [:div [:h2 "Playlist"]
       [:ul (for [track @playlist]
              [:li (track-span track)])]])))

(defn enqueue-button [track]
  [:input {:type "button" :value "Enqueue"
           :on-click #(enqueue-track track)}])

(defn query-input [query]
  [:input {:type "text"
           :value @query
           :on-change #(reset! query
                               (-> % .-target .-value))}])

(defn tracks-list [tracks]
  [:ul (for [track @tracks]
         [:li
          [enqueue-button track]
          [track-span track]])])

(defn upload-form []
  [:form {:enc-type "multipart/form-data"
          :method "POST" :action "/api/tracks"}
   [:input {:type "file" :name "track"}]
   [:input {:type "submit" :value "Upload"}]])

(defn tracks-section []
  (let [tracks (r/atom [])
        query (r/atom "")
        reset-tracks #(reset! tracks (% "items"))
        reset-query #(reset! query (-> % .-target .-value))
        get-tracks (fn []
                     (get-json "/api/tracks" reset-tracks
                               {:q @query}))]
    (fn []
      (js/setTimeout get-tracks 1000)
      [:div [:h2 "Tracks"]
       [query-input query]
       [tracks-list tracks]
       [upload-form]])))

(defn home-page []
  [:div [:h1 "Airhead"]
   [:audio {:src "http://localhost:8000/airhead"
            :controls "controls"}]
   [playlist-section]
   [tracks-section]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(ns airhead-frontend.components
  (:require [reagent.core :as r]
            [airhead-frontend.state :refer [app-state update-state!]]
            [airhead-frontend.requests :as req]))

(defn header []
  (let [cursor  (r/cursor app-state [:info])
        title   (@cursor :name)
        message (@cursor :greet_message)]
    [:header
     [:h1 title]
     [:p message]]))

(defn now-playing []
  (let [track (@app-state :now-playing)]
    [:p#now-playing
     [:span.note-icon]
     (if track
       (str " " (:artist track) " - " (:title track))
       [:em "Nothing is playing"])]))

(defn player-section []
  (let [cursor  (r/cursor app-state [:info])
        url     (@cursor :stream_url)]
    [:section#player
     (when url
       [:div
        [:audio {:controls "controls"}
         [:source {:src url}]]
        [:a {:href url}]])
     [now-playing]]))

(defn upload-section []
  [:section#upload
    [:h2 "Upload"]
    [:form {:id "upload-form"}
           [:input {:type "file" :name "track"}]
           [:input {:type "button" :value "Upload" :on-click req/upload!}]
           [:progress {:max 100 :value (@app-state :upload-percentage)}]]
    [:p (@app-state :upload-status)]])

(defn playlist-add-button [track]
  [:button.add
   {:on-click #(req/playlist-add! (:uuid track))}])

(defn playlist-remove-button [track]
  [:button.remove
   {:on-click #(req/playlist-remove! (:uuid track))}])

(defn track-tr [track action-button]
  [:tr.track
   [:td
    (when action-button
      [action-button track])]
   [:td (track :title)] [:td (track :artist)] [:td (track :album)]])

(defn tracks-table [tracks action-button]
  [:table.tracks
   [:thead
    [:tr [:th] [:th  "Title"] [:th "Artist"] [:th "Album"]]]
   [:tbody (for [track tracks]
             ^{:key track} [track-tr track action-button])]])

(defn playlist-section []
  [:section#playlist
   [:h2 "Playlist"]
   [tracks-table (@app-state :playlist) playlist-remove-button]])

(defn on-query-change [e]
  ;(get-library!)
  (update-state! :query (-> e .-target .-value)))

(defn search-form []
  [:section#search
   [:form
    [:label {:for "query"} "Search:"]
    [:input {:type "text"
             :id "query"
             :value (@app-state :query)
             :on-change on-query-change}]]])

(defn library-section []
  [:section#library
   [:h2 "Library"]
   [search-form]
   [tracks-table (@app-state :library) playlist-add-button]])

(defn page-component []
  [:main
   [header]
   [player-section]
   [upload-section]
   [playlist-section]
   [library-section]])

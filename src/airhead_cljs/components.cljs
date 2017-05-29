(ns airhead-cljs.components
  (:require [reagent.core :as r]
            [airhead-cljs.state :refer [app-state update-state!]]
            [airhead-cljs.requests :as req]))

(defn now-playing []
  (let [track (@app-state :now-playing)]
    [:p#now-playing
     [:span "Now playing:"]
     (if track
       (str " " (:artist track) " - " (:title track))
       "-")]))

(defn header []
  (let [cursor  (r/cursor app-state [:info])
        title   (@cursor :name)
        message (@cursor :greet_message)
        url     (@cursor :stream_url)]
    [:header
     [:h1 title]
     [:p message]
     [:audio {:controls "controls"}
      [:source {:src url}]]
     [now-playing]]))

(defn upload-section []
  [:section#upload
   [:h2 "Upload"]
   [:form {:id "upload-form"}
    [:input {:type "file" :name "track"}]
    [:input {:type "button" :value "Upload" :on-click req/upload!}]]])

(defn playlist-add-button [track]
  [:input.add
   {:type "button" :value "+"
    :on-click #(req/playlist-add! (:uuid track))}])

(defn playlist-remove-button [track]
  [:input.remove
   {:type "button" :value "-"
    :on-click #(req/playlist-remove! (:uuid track))}])

(defn track-tr [track action-button]
  [:tr.track
   (when action-button
     [action-button])
   [:td ]
   [:td (track :title)] [:td (track :artist)] [:td (track :album)]])

(defn tracks-table [tracks action-button]
  [:table.tracks
   [:thead
    [:tr [:th] [:th  "Title"] [:th "Artist"] [:th "Album"]]]
   [:tbody (for [track tracks]
             [track-tr track action-button])]])

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
   [upload-section]
   [playlist-section]
   [library-section]])

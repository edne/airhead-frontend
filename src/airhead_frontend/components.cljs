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

(defn player-section []
  (let [cursor  (r/cursor app-state [:info])
        track   (@app-state :now-playing)
        url     (@cursor :stream_url)
        ;; TODO: do not use Id
        get-audio-tag #(js/document.getElementById "audio-element")]
    [:section#player
     (when url
       [:div
        [:audio#audio-element {:controls "controls"
                               :style {:display "none"}}
         [:source {:src url}]]
        (let []
          [:span#controls
           ;; TODO: show just one of two
           [:button {:on-click #(.play (get-audio-tag))}  "⏵"]
           [:button {:on-click #(.pause (get-audio-tag))} "⏸"]])

        (if track
          (str (:artist track) " - " (:title track))
          [:em "Nothing is playing"])
        [:a {:href url} "↗"]])]))

(defn upload-section []
  [:section#upload
    [:h2 "Upload"]

    [:form {:id "upload-form"}
           [:label "📂 Choose a file"
            [:input {:type "file" :name "track" :on-change req/upload!
                     :style {:display "none"}}]]
           [:progress {:max 100 :value (@app-state :upload-percentage)}]]

    [:p (@app-state :upload-status)]])

(defn playlist-add-button [track]
  [:button.add
   {:on-click #(req/playlist-add! (:uuid track))}
   "+"])

(defn playlist-remove-button [track]
  [:button.remove
   {:on-click #(req/playlist-remove! (:uuid track))}
   "-"])

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

(defn update-sort-field! [new-field]
  (if (= new-field (:sort-field @app-state))
    (swap! app-state update-in [:ascending] not)
    (swap! app-state assoc :ascending true))
  (swap! app-state assoc :sort-field new-field))

(defn sort-tracks [tracks]
  (let [sorted-tracks (sort-by (:sort-field @app-state) tracks)]
    (if (:ascending @app-state)
      sorted-tracks
      (rseq sorted-tracks))))

(defn sort-button [field]
  [:button.sort
   {:on-click #(update-sort-field! field)} "⇅"])

(defn sorted-tracks-table [tracks action-button]
  [:table.tracks
   [:thead
    [:tr [:th]
     [:th "Title" [sort-button :title]]
     [:th "Artist" [sort-button :artist]]
     [:th "Album" [sort-button :album]]]]
   [:tbody (for [track (sort-tracks tracks)]
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
   [sorted-tracks-table (@app-state :library) playlist-add-button]])

(defn page-component []
  [:main
   [header]
   [player-section]
   [upload-section]
   [playlist-section]
   [library-section]])

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
    [:span (if track
             (str (:artist track) " - " (:title track))
             [:em "Nothing is playing"])]))

(defn player-section []
  (let [audio-ref (r/atom nil)]
    (fn []
      (when-let [url (get-in @app-state [:info :stream_url])]
        [:section#player

         [:audio {:ref #(reset! audio-ref %) :style {:display "none"}}
          [:source {:src url}]]

         [:span#controls
          (when-let [audio @audio-ref]
            (if (.-paused audio)
              [:button {:on-click #(.play audio)}  "⏵"]
              [:button {:on-click #(.pause audio)} "⏸"]))]

         [now-playing]
         [:a {:href url} "↗"]]))))

(defn progress-bar []
  (let [percentage (@app-state :upload-percentage)]
    (if (< 0 percentage 100)
      [:progress {:max 100 :value percentage}])))

(defn upload-section []
  (let [form-ref (r/atom nil)]
    (fn []
      [:section#upload
       [:h2 "Upload"]

       [:form {:ref #(reset! form-ref %)}
        [:label "📂 Choose a file"
         [:input {:type "file" :name "track"
                  :on-change #(when-let [form @form-ref]
                                (req/upload! form))
                  :style {:display "none"}}]]
        [progress-bar]]

       [:p (@app-state :upload-status)]])))

(defn playlist-add-button [track]
  [:button.add
   {:on-click #(req/playlist-add! (:uuid track))}
   "[+]"])

(defn playlist-remove-button [track]
  [:button.remove
   {:on-click #(req/playlist-remove! (:uuid track))}
   "[-]"])

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

(defn sorting-th [field caption]
  [:th {:on-click #(update-sort-field! field)}
   caption
   [:span.sorting-arrow (when (= field (@app-state :sort-field))
                          (if (@app-state :ascending)
                            " ▲"
                            " ▼"))]])

(defn sorted-tracks-table [tracks action-button]
  [:table.tracks
   [:thead
    [:tr [:th]
     [sorting-th :title "Title"]
     [sorting-th :artist "Artist"]
     [sorting-th :album "Album"]]]
   [:tbody (for [track (sort-tracks tracks)]
             ^{:key track} [track-tr track action-button])]])

(defn playlist-section []
  [:section#playlist
   [:h2 "Playlist"]
   (if-let [tracks (not-empty (@app-state :playlist))]
     [tracks-table tracks playlist-remove-button]
     "The playlist is empty")])

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

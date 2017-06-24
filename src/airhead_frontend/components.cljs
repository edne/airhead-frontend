(ns airhead-frontend.components
  (:require [reagent.core :as r]
            [airhead-frontend.state :refer [app-state update-state!]]
            [airhead-frontend.requests :as req]))

;; -------------------------
;; Header

(defn header []
  (let [cursor  (r/cursor app-state [:info])
        title   (@cursor :name)
        message (@cursor :greet_message)]
    [:header
     [:h1 title]
     [:p message]]))

;; -------------------------
;; Player

(defn now-playing []
  (let [track (@app-state :now-playing)]
    [:p#player-track
     [:i.fa.fa-music]
     [:span (if track
              (str (:artist track) " - " (:title track))
              [:em "Nothing is playing."])]]))

(defn player-section []
  (let [audio-ref (r/atom nil)]
    (fn []
      (when-let [url (get-in @app-state [:info :stream_url])]
        [:section#player
         [:audio {:ref #(reset! audio-ref %)}
          [:source {:src url}]]

         [:div#player-controls.pure-button-group
          {:role "group"}

          (when-let [audio @audio-ref]
            (if (.-paused audio)
              [:button.pure-button
               {:on-click #(.play audio)}
               [:i.fa.fa-play]
               [:span "Play"]]
              [:button.pure-button.pure-button-active
               {:on-click #(.pause audio)}
               [:i.fa.fa-pause]
               [:span "Pause"]]))

          (when-let [audio @audio-ref]
            (if (.-muted audio)
              [:button.pure-button.pure-button-active
               {:on-click #(set! (.-muted audio) false)}
               [:i.fa.fa-volume-off]]
              [:button.pure-button
               {:on-click #(set! (.-muted audio) true)}
               [:i.fa.fa-volume-up]]))


          [:a.pure-button {:href url :target "_blank"}
           [:i.fa.fa-external-link]
           [:span "Open stream"]]]

         [now-playing]]))))

;; -------------------------
;; Upload

(defn progress-bar []
  (let [percentage (@app-state :upload-percentage)]
    (if (< 0 percentage 100)
      [:progress.pure-input-1 {:max 100 :value percentage}])))

(defn upload-section []
  (let [form-ref (r/atom nil)]
    (fn []
      [:section#upload
       [:h2 "Upload"]

       [:form.pure-form
        {:ref #(reset! form-ref %)}
        [:input.pure-input-2-3 {:type "file" :name "track"}]
        [:input.pure-button.pure-input-1-3
         {:type "button" :value "Upload"
          :on-click #(when-let [form @form-ref] (req/upload! form))}]
        [progress-bar]]

       [:p (@app-state :upload-status)]])))

;; -------------------------
;; Tracks

(defn playlist-add-button [track]
  [:button.pure-button.track-action
   {:on-click #(req/playlist-add! (:uuid track))}
   [:i.fa.fa-plus]])

(defn playlist-remove-button [track]
  [:button.pure-button.track-action
   {:on-click #(req/playlist-remove! (:uuid track))}
   [:i.fa.fa-minus]])

(defn track-tr [track action-button]
  [:tr.track
   [:td
    (when action-button
      [action-button track])]
   [:td (track :title)] [:td (track :artist)] [:td (track :album)]])


;; -------------------------
;; Tables

(defn table [head content]
  [:table.pure-table.pure-table-horizontal
   [:thead head]
   [:tbody content]])

;; -------------------------
;; Playlist

(defn playlist-section []
  [:section#playlist
   [:h2 "Playlist"]
   (if-let [tracks (not-empty (@app-state :playlist))]
     [table
      [:tr [:th] [:th  "Title"] [:th "Artist"] [:th "Album"]]
      (for [track tracks]
        ^{:key track} [track-tr track playlist-remove-button])]
     "The playlist is empty.")])

;; -------------------------
;; Search

(defn on-query-change [e]
  (update-state! :query (-> e .-target .-value))
  (req/get-library!))

;; -------------------------
;; Library

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
  [:th.sorting-th {:on-click #(update-sort-field! field)}
   [:span caption]
   [:span.sorting-th-arrow (when (= field (@app-state :sort-field))
                          (if (@app-state :ascending)
                            "▲"
                            "▼"))]])

(defn library-section []
  (let [tracks (@app-state :library)]
    [:section#library
     [:h2 "Library"]
     [:form#library-search.pure-form
      [:input {:type "text"
               :id "query"
               :placeholder "Search through the library..."
               :value (@app-state :query)
               :on-change on-query-change}]]
     [table
      [:tr [:th]
       [sorting-th :title "Title"]
       [sorting-th :artist "Artist"]
       [sorting-th :album "Album"]]
      (for [track (sort-tracks tracks)]
        ^{:key track} [track-tr track playlist-add-button])]]))

;; -------------------------
;; Main

(defn page-component []
  [:main
   [header]
   [player-section]
   [upload-section]
   [playlist-section]
   [library-section]])

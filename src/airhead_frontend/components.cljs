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
              [:button.pure-button.pure-button-primary
               {:on-click #(.play audio)}
               [:i.fa.fa-play]
               [:span "Play"]]
              [:button.pure-button.pure-button-primary.pure-button-active
               {:on-click #(.pause audio)}
               [:i.fa.fa-pause]
               [:span "Pause"]]))

          (when-let [audio @audio-ref]
            (if (.-muted audio)
              [:button.pure-button.pure-button-active
               {:on-click (fn [] (set! (.-muted audio) false))}
               [:i.fa.fa-volume-off]]
              [:button.pure-button
               {:on-click (fn [] (set! (.-muted audio) true))}
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
        [:input.pure-input-3-4 {:type "file" :name "track"}]
        [:input.pure-button.pure-button-primary.pure-input-1-4
         {:type "button" :value "Upload"
          :on-click #(when-let [form @form-ref] (req/upload! form))}]
        [progress-bar]]

       [:p (@app-state :upload-status)]])))

;; -------------------------
;; Tracks

(defn playlist-add-button [track]
  [:i.fa.fa-plus-square
   {:on-click #(req/playlist-add! (:uuid track))}])

(defn playlist-remove-button [track]
  [:i.fa.fa-minus-square
   {:on-click #(req/playlist-remove! (:uuid track))}])

(defn track-tr [track action-button]
  [:tr.track
   [:td
    (when action-button
      [action-button track])]
   [:td (track :title)] [:td (track :artist)] [:td (track :album)]])

;; -------------------------
;; Playlist

(defn playlist-section []
  [:section#playlist
   [:h2 "Playlist"]
   (if-let [tracks (not-empty (@app-state :playlist))]
     [:table.pure-table.pure-table-striped
      [:thead
       [:tr [:th] [:th  "Title"] [:th "Artist"] [:th "Album"]]]
      [:tbody (for [track tracks]
                ^{:key track} [track-tr track playlist-remove-button])]]
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
  [:th {:on-click #(update-sort-field! field)}
   caption
   [:span.sorting-arrow (when (= field (@app-state :sort-field))
                          (if (@app-state :ascending)
                            " ▲"
                            " ▼"))]])

(defn library-section []
  (let [tracks (@app-state :library)]
    [:section#library
     [:h2 "Library"]
     [:form#library-search.pure-form
      [:input {:type "text"
               :id "query"
               :placeholder "Search through the library..."
               :value (@app-state :query)
               :on-change on-query-change}]
      [:span (str "Tracks: " (count tracks))]]
     [:table.pure-table.pure-table-striped
      [:thead
       [:tr [:th]
        [sorting-th :title "Title"]
        [sorting-th :artist "Artist"]
        [sorting-th :album "Album"]]]
      [:tbody (for [track (sort-tracks tracks)]
                ^{:key track} [track-tr track playlist-add-button])]]]))

;; -------------------------
;; Main

(defn page-component []
  [:main
   [header]
   [player-section]
   [upload-section]
   [:div.pure-u-1.pure-u-md-1-2
    [playlist-section]]
   [:div.pure-u-1.pure-u-md-1-2
    [library-section]]])

(ns airhead-frontend.components
  (:require [clojure.string :refer [blank? split lower-case]]
            [reagent.core :as r]
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

(defn pause-button [audio]
  [:button.pure-button.pure-button-active.pure-u-1-3
   {:title "Pause"
    :on-click #(.pause audio)}
   [:i.fa.fa-pause]])

(defn play-button [audio]
  [:button.pure-button.pure-u-1-3
   {:title "Play"
    :on-click #(.play audio)}
   [:i.fa.fa-play]])

(defn audio-on-button [audio]
  [:button.pure-button.pure-u-1-3
   {:title "Mute"
    :on-click #(set! (.-muted audio) true)}
   [:i.fa.fa-volume-up]])

(defn audio-off-button [audio]
  [:button.pure-button.pure-button-active.pure-u-1-3
   {:title "Unmute"
    :on-click #(set! (.-muted audio) false)}
   [:i.fa.fa-volume-off]])

(defn open-stream-button [url]
  [:a.pure-button.pure-u-1-3
   {:title "Open stream"
    :href url :target "_blank"}
   [:i.fa.fa-external-link]])

(defn now-playing []
  (let [track (@app-state :now-playing)]
    [:div
     [:i.fa.fa-music]
     [:span (if track
              (str (:artist track) " - " (:title track))
              [:em "Nothing is playing."])]]))

(defn player-section []
  (let [audio-ref (r/atom nil)]
    (fn []
      (when-let [url (get-in @app-state [:info :stream_url])]
        [:section#player
         [:audio.hidden {:ref #(reset! audio-ref %)}
          [:source {:src url}]]

         (when-let [audio @audio-ref]
           [:div.controller-box
            [:div.pure-button-group
             {:role "group"}

             (if (.-paused audio)
               [play-button audio]
               [pause-button audio])

             (if (.-muted audio)
               [audio-off-button audio]
               [audio-on-button audio])

             [open-stream-button url]]

            [now-playing]])]))))

;; -------------------------
;; Upload

(defn upload-section []
  (let [form-ref         (r/atom nil)
        file-input-ref   (r/atom nil)

        state (r/atom {:response nil
                       :percentage 0})]
    (fn []
      [:section#upload
       [:h2 "Upload"]

       [:form.hidden {:ref #(reset! form-ref %)}

        [:input {:type "file" :name "track"
                 :ref #(reset! file-input-ref %)}]]

       [:div.controller-box
        [:div.pure-button-group
         [:div.pure-button.pure-u-1-2
          {:title "Select a file"
           :on-click #(when @file-input-ref
                        (.click @file-input-ref))}
          [:i.fa.fa-folder-open]]

         [:div.pure-button.pure-button.pure-u-1-2
          {:title "Upload"
           :on-click #(when @file-input-ref
                        (when-let [form @form-ref]
                          (req/upload! form state)))}
          [:i.fa.fa-upload]]]

        [:div
         [:i.fa.fa-file-o]
         [:span (when @file-input-ref
                 (let [path (.-value @file-input-ref)]
                   (if-not (blank? path)
                     (-> path (split "\\") last)  ; C:\fakepath\file-name
                     "No file selected.")))]]]

       (let [percentage (@state :percentage)]
         (if (< 0 percentage 100)
           [:progress.pure-input-1 {:max 100 :value percentage}]))

       (when-let [response (@state :response)]
         (let [{status :status
                {error-msg :msg
                 track     :track} :body} response]
           [:p (if (= status 200)
                 ; TODO: show when the track appears in the library
                 "Done! It will show in the library once it gets transcoded."
                 error-msg)]))])))

;; -------------------------
;; Tracks

(defn playlist-add-button [track]
  [:button.pure-button.track-action
   {:title "Add to playlist"
    :on-click #(req/playlist-add! (:uuid track))}
   [:i.fa.fa-plus]])

(defn playlist-remove-button [track]
  [:button.pure-button.track-action
   {:title "Remove from playlist"
    :on-click #(req/playlist-remove! (:uuid track))}
   [:i.fa.fa-minus]])

(defn track-field [text]
  [:td {:title text} text])

(defn track-tr [track action-button]
  [:tr.track
   [:td
    (when action-button
      [action-button track])]
   [track-field (track :title)]
   [track-field (track :artist)]
   [track-field (track :album)]])


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
  [:th.sorting-th {:title (str "Sort by " (lower-case caption))
                   :on-click #(update-sort-field! field)}
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

(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))

(defn get-json
  ([url handler] (get-json url handler []))
  ([url handler params] (GET url {:handler handler
                                  :params params
                                  :response-format :json})))

;; -------------------------
;; Views

(defn track-span [track]
  [:span (str (track "artist") " - " (track "title"))])


(defn stream-section [url]
  [:div
   [:audio {:src url :controls "controls"}]])


(defn current-track-section []
  (let [current-track (atom {})
        reset         #(reset! current-track (% "track"))
        get-track     #(get-json "/api/queue/current" reset)]
    (fn []
      (js/setTimeout get-track 1000)
      [:div
       [:strong "Now playing: "]
       [track-span @current-track]])))

(defn dequeue-button [track]
  [:input {:type "button" :value "Dequeue"
           :on-click #(DELETE (str "/api/queue/"
                                   (track "uuid")))}])

(defn playlist-section []
  (let [playlist     (atom [])
        reset        #(reset! playlist (% "items"))
        get-playlist #(get-json "/api/queue" reset)]
    (fn []
      (js/setTimeout get-playlist 1000)
      [:div
       [:h2 "Playlist"]
       [:ul (for [track @playlist]
              [:li
               (dequeue-button track)
               (track-span track)])]])))

(defn enqueue-button [track]
  [:input {:type "button" :value "Enqueue"
           :on-click #(PUT (str "/api/queue/"
                                (track "uuid")))}])

(defn tracks-list [tracks]
  [:ul (for [track @tracks]
         [:li
          [enqueue-button track]
          [track-span track]])])

(defn post-track [status]
  ;; TODO: do not use element id
  (let [form (.getElementById js/document
                              "upload-form")
        on-success (fn []
                     (reset! status "Done!")
                     (.reset form))
        on-error #(reset! status "Something went wrong")]
    (reset! status "Uploading...")
    (POST "/api/tracks" {:enc-type "multipart/form-data"
                         :body (js/FormData. form)
                         :handler on-success
                         :error-handler on-error})))

(defn upload-section []
  (let [upload-status (atom "")]
    (fn []
      [:div
       [:h2 "Upload"]
       [:form {:id "upload-form"}
        [:input {:type "file"
                 :name "track"}]
        [:input {:type "button"
                 :value "Upload"
                 :on-click #(post-track upload-status)}]]
       [:div @upload-status]])))

(defn tracks-section []
  (let [tracks (atom [])
        reset-tracks #(reset! tracks (% "items"))
        get-tracks   #(get-json "/api/tracks"
                                reset-tracks
                                {:q %})
        query  (atom "")
        reset-query (fn [e]
                      (reset! query (-> e .-target .-value))
                      (get-tracks @query))]
    (get-tracks @query)
    (fn []
      [:div
       [:h2 "Tracks"]
       [:input {:type "text"
                :value @query
                :on-change reset-query}]
       [tracks-list tracks]])))

(defn home-page []
  (let [info (atom {})]
    (get-json "/api/info" #(reset! info %))
    (fn []
      [:div
       [:h1 (@info "name")]
       [:p  (@info "greet_message")]
       [stream-section (@info "stream_url")]
       [current-track-section]
       [upload-section]
       [playlist-section]
       [tracks-section]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT]]))

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

(defn playlist-section []
  (let [playlist     (atom [])
        reset        #(reset! playlist (% "items"))
        get-playlist #(get-json "/api/queue" reset)]
    (fn []
      (js/setTimeout get-playlist 1000)
      [:div
       [:h2 "Playlist"]
       [:ul (for [track @playlist]
              [:li (track-span track)])]])))

(defn enqueue-button [track]
  [:input {:type "button" :value "Enqueue"
           :on-click #(PUT (str "/api/queue/"
                                (track "uuid")))}])

(defn tracks-list [tracks]
  [:ul (for [track @tracks]
         [:li
          [enqueue-button track]
          [track-span track]])])

(defn post-track []
  ;; TODO: do not use element id
  (let [form (.getElementById js/document
                              "upload-form")]
    (POST "/api/tracks" {:enc-type "multipart/form-data"
                         :body (js/FormData. form)})))

(defn upload-form []
  [:form {:id "upload-form"}
   [:input {:type "file"
            :name "track"}]
   [:input {:type "button"
            :value "Upload"
            :on-click post-track}]])

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
       [tracks-list tracks]
       [upload-form]])))

(defn home-page []
  (let [info (atom {})]
    (get-json "/api/info" #(reset! info %))
    (fn []
      [:div
       [:h1 (@info "name")]
       [:p  (@info "greet_message")]
       [stream-section (@info "stream_url")]
       [current-track-section]
       [playlist-section]
       [tracks-section]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

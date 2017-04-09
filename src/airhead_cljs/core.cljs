(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET PUT]]))

(def state (r/atom {:playlist []
                    :tracks []
                    :query ""}))

(defn update-state! [k value]
  (swap! state assoc k value))

(defn get-json [url handler params]
  (GET url {:handler handler
            :params params
            :response-format :json}))

(defn enqueue-track [track]
  (let [uuid (track "uuid")]
    (PUT (str "/api/enqueue/" uuid))))

;; -------------------------
;; Views

(defn track-span [track]
  (str (track "artist") " - " (track "title")))

(defn playlist-section []
  (let [get-playlist  (fn []
                        (get-json "/api/queue"
                                  #(update-state! :playlist (% "items"))
                                  {}))]

    (js/setTimeout get-playlist 1000)
    [:div [:h2 "Playlist"]
     [:ul (for [track (@state :playlist)]
            [:li (track-span track)])]]))

(defn enqueue-button [track]
  [:input {:type "button" :value "Enqueue"
           :on-click #(enqueue-track track)}])

(defn search-box []
  [:input {:type "text"
           :value (@state :query)
           :on-change #(update-state! :query
                                      (-> % .-target .-value))}])
(defn tracks-section []
  (let [get-tracks (fn []
                     (get-json "/api/tracks"
                               #(update-state! :tracks (% "items"))
                               {:q (@state :query)}))]


    (js/setTimeout get-tracks 1000)
    [:div [:h2 "Tracks"]
     [search-box]
     [:ul (for [track (@state :tracks)]
            [:li
             (enqueue-button track)
             (track-span track)])]]))

(defn upload-form []
  [:form {:enc-type "multipart/form-data"
          :method "POST" :action "/api/upload"}
   [:input {:type "file" :name "track"}]
   [:input {:type "submit"}]])

(defn upload-section []
  [:div
   [:h2 "Upload"]
   [upload-form]])

(defn home-page []
  [:div [:h1 "Airhead"]
   [playlist-section]
   [tracks-section]
   [upload-section]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

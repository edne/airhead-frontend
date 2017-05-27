(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}
                  :upload-status ""
                  :playlist []
                  :library []}))

;; -------------------------
;;

(defn get-info! []
  (GET "/api/info"
       {:handler #(swap! state assoc :info %)
        :response-format :json}))

(defn get-playlist! []
  (GET "/api/playlist"
       {:handler #(swap! state assoc :playlist %)
        :response-format :json}))

(defn playlist-add [id]
  (PUT (str "/api/playlist/" id)))

(defn playlist-remove [id]
  (DELETE (str "/api/playlist/" id)))

(defn get-library! []
  (GET "/api/library"
       {:handler #(swap! state assoc :library (% "tracks"))
        :params {:q (@state :query)}
        :response-format :json}))

(defn on-form-success [form]
  (swap! state assoc :upload-status "Done!")
  (.reset form))

(defn on-form-error [form]
  (swap! state assoc :upload-status "Something went wrong"))

(defn upload! []
  ;; TODO: do not use element id
  (let [form (.getElementById js/document
                              "upload-form")]
    (swap! state assoc :upload-status "Uploading...")
    (POST "/api/library" {:enc-type "multipart/form-data"
                          :body (js/FormData. form)
                          :handler #(on-form-success form)
                          :error-handler #(on-form-error form)
                          :query ""})))

(defn polling-callback []
  (get-info!)
  (get-playlist!)
  (get-library!))

(js/setInterval polling-callback 1000)

;; -------------------------
;; Views

(defn info-component []
  (fn []
    (let [title   (get-in @state [:info "name"])
          message (get-in @state [:info "greet_message"])
          url     (get-in @state [:info "stream_url"])]
      [:section#info
       [:h1 title]
       [:p message]
       [:audio {:src url
                :controls "controls"}]])))

(defn upload-component []
  [:section#upload
   [:h2 "Upload"]
   [:form {:id "upload"}
    [:input {:type "file" :name "track"}]
    [:input {:type "button" :value "Upload" :on-submit upload!}]]
   [:p (@state :upload-status)]])

(defn playlist-add-component [track]
  [:input.add
   {:type "button" :value "+"
    :on-click #(playlist-add (track "uuid"))}])

(defn playlist-remove-component [track]
  [:input.remove
   {:type "button" :value "-"
    :on-click #(playlist-remove (track "uuid"))}])

(defn track-component [track]
  [:span.track
   (if track
     (str " " (track "artist") " - " (track "title"))
     "-")])

(defn now-playing-component []
  [:p
   [:span "Now playing:"]
   [track-component (get-in @state [:playlist "current"])]])

(defn next-component []
  [:section
   [:span "Next:"]
   [:ul
    (for [track (get-in @state [:playlist "next"])]
      [:li
       [playlist-remove-component track]
       [track-component track]])]])

(defn playlist-component []
  [:section#playlist
   [:h2 "Playlist"]
   [now-playing-component]
   [next-component]])

(defn on-query-change [e]
  ;(get-library!)
  (swap! state assoc :query (-> e .-target .-value)))

(defn search-component []
  [:section#search
   [:form
    [:label {:for "query"} "Search:"]
    [:input {:type "text"
             :id "query"
             :value (@state :query)
             :on-change on-query-change}]]])

(defn library-component []
  [:section#library
   [:h2 "Library"]
   [search-component]
   [:ul (for [track (@state :library)]
          [:li
           [playlist-add-component track]
           [track-component track]])]])

(defn page-component []
  [:main
   [info-component]
   [upload-component]
   [playlist-component]
   [library-component]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

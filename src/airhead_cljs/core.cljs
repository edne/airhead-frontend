(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))


(def state (atom {:info {:name          ""
                         :greet_message ""
                         :stream_url    ""}
                  :playlist []
                  :library []}))

;; -------------------------
;; Requests

;; TODO: handle error responses

(defn get-info! []
  (go (let [response (<! (http/get "/api/info"))]
        (swap! state assoc :info (:body response)))))

(defn get-playlist! []
  (go (let [response (<! (http/get "/api/playlist"))]
        (swap! state assoc :playlist (:body response)))))

(defn playlist-add [id]
  (http/put (str "/api/playlist/" id)))

(defn playlist-remove [id]
  (http/delete (str "/api/playlist/" id)))

(defn get-library! []
  (go (let [response (<! (http/get "/api/library"
                                   {:query-params {"q" (@state :query)}}))]
        (swap! state assoc :library (get-in response [:body :tracks])))))

(defn upload! []
  ;; TODO: do not use element id
  (let [form (.getElementById js/document
                              "upload-form")]
    ;; TODO: progress bar
    (http/post "/api/library" {:body (js/FormData. form)})))

(defn polling-callback []
  (get-info!)
  (get-playlist!)
  (get-library!))

(js/setInterval polling-callback 1000)

;; -------------------------
;; Views

(defn info-component []
  (fn []
    (let [title   (get-in @state [:info :name])
          message (get-in @state [:info :greet_message])
          url     (get-in @state [:info :stream_url])]
      [:section#info
       [:h1 title]
       [:p message]
       [:audio {:src url
                :controls "controls"}]])))

(defn upload-component []
  [:section#upload
   [:h2 "Upload"]
   [:form {:id "upload-form"}
    [:input {:type "file" :name "track"}]
    [:input {:type "button" :value "Upload" :on-click upload!}]]])

(defn playlist-add-component [track]
  [:input.add
   {:type "button" :value "+"
    :on-click #(playlist-add (:uuid track))}])

(defn playlist-remove-component [track]
  [:input.remove
   {:type "button" :value "-"
    :on-click #(playlist-remove (:uuid track))}])

(defn track-component [track]
  [:span.track
   (if track
     (str " " (:artist track) " - " (:title track))
     "-")])

(defn now-playing-component []
  [:p
   [:span "Now playing:"]
   [track-component (get-in @state [:playlist :current])]])

(defn next-component []
  [:section
   [:span "Next:"]
   [:ul
    (for [track (get-in @state [:playlist :next])]
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

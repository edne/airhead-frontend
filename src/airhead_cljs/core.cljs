(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(def state (r/atom {:server "http://localhost:8080"}))


(defn change-server! [event]
  (swap! state
         assoc :server
         (-> event .-target .-value)))

;; -------------------------
;; Views

(defn server-form []
  [:div
   [:input {:type "text"
            :value (:server @state)
            :on-change change-server!}]])

(defn playlist-div []
  (let [playlist (r/atom [])
        handler #(reset! playlist (% "items"))]
    (GET "http://localhost:8080/api/queue"
         {:handler handler
          :response-format :json})
    (fn []
      [:div [:h2 "Playlist"]
       [:ul (for [track @playlist]
              [:li (track "title")])]])))

(defn home-page []
  [:div [:h1 "Airhead"]
   [server-form]
   [playlist-div]
   ])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

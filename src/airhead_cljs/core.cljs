(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]))

(def state (r/atom {:server "http://localhost:8080"
                    :playlist []}))

(defn update-state! [k value]
  (swap! state assoc k value))

;; -------------------------
;; Views

(defn server-form []
  [:div
   [:input {:type "text"
            :value (@state :server)
            :on-change #(update-state! :server
                                       (-> % .-target .-value))}]])

(defn playlist-div []
  (GET "http://localhost:8080/api/queue"
       {:handler #(update-state! :playlist (% "items"))
        :response-format :json})
  (fn []
    [:div [:h2 "Playlist"]
     [:ul (for [track (@state :playlist)]
            [:li (track "title")])]]))

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

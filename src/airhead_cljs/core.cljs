(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}
                  :library {}}))

;; -------------------------
;; Views

(defn info-component []
  (let [title   (get-in @state [:info "name"])
        message (get-in @state [:info "greet_message"])
        url     (get-in @state [:info "stream_url"])]
    [:div.info
     [:h1 title]
     [:div.greet-message message]
     [:div.player [:audio {:src url
                           :controls "controls"}]]]))

(defn track-component [track]
  [:span (str (track "artist") " - " (track "title"))])

(defn library-component []
  (let [tracks (-> @state
                   (get-in [:library "tracks"])
                   seq)]
    [:div.library
     [:h2 "Library"]
     [:ul (for [[id track] tracks]
            [:li [track-component track]])]]))

(defn page-component []
  [:div.page
   [info-component]
   [library-component]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (GET "/api/info"
       {:handler #(swap! state assoc :info %)
        :response-format :json})
  (GET "/api/library"
       {:handler #(swap! state assoc :library %)
        :response-format :json})

  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST PUT DELETE]]))


; TODO: use data.json to get map keys as :keywords
(def state (atom {:info {"name"          ""
                         "greet_message" ""
                         "stream_url"    ""}}))

;; -------------------------
;; Views

(defn info []
  (let [title   (get-in @state [:info "name"])
        message (get-in @state [:info "greet_message"])
        url     (get-in @state [:info "stream_url"])]

    [:div.info
     [:h1 title]
     [:div.greet-message message]
     [:div.player [:audio {:src url
                           :controls "controls"}]]]))

(defn page []
  [:div.page
   [info]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (GET "/api/info" {:handler #(swap! state assoc :info %)
                    :response-format :json})

  (r/render [page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

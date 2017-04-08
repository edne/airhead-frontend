(ns airhead-cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]))

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

(defn home-page []
  [:div [:h1 "Airhead"]
   [server-form]])

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

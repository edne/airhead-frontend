(ns airhead-cljs.core
  (:require [reagent.core :as r]
            [airhead-cljs.components :refer [page-component]]))

(def app-state (r/atom {:info {:name          ""
                               :greet_message ""
                               :stream_url    ""}
                        :playlist []
                        :now-playing nil
                        :library []}))

(defn update-state! [k v]
  (swap! app-state assoc k v))

(defn mount-root []
  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

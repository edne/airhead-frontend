(ns airhead-cljs.core
  (:require [reagent.core :as r]
            [airhead-cljs.components :refer [page-component]]))

(defn mount-root []
  (r/render [page-component] (.getElementById js/document "app")))

(defn init! []
  (mount-root))

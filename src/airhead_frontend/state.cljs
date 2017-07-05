(ns airhead-frontend.state
  (:require [reagent.core :as r]))

(def app-state (r/atom {:info {:name          ""
                               :greet_message ""
                               :stream_url    nil}

                        :playlist []
                        :now-playing nil

                        :library []
                        :sort-field :title
                        :ascending true

                        :upload-percentage 0
                        :upload-response nil}))

(defn update-state! [k v]
  (swap! app-state assoc k v))

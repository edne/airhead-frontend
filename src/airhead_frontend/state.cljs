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

                        ;; TODO :upload branch
                        :upload-percentage 0
                        :upload-status "Ready to upload."}))

(defn update-state! [k v]
  (swap! app-state assoc k v))

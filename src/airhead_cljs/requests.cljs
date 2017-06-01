(ns airhead-cljs.requests
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]
            [airhead-cljs.state :refer [app-state update-state!]]))

;; TODO: handle error responses

(defn get-info! []
  (go (let [response (<! (http/get "/api/info"))]
        (update-state! :info (:body response)))))

(defn get-playlist! []
  (go (let [response (<! (http/get "/api/playlist"))
            body     (response :body)]
        (update-state! :playlist    (body :next))
        (update-state! :now-playing (body :current)))))

(defn playlist-add! [id]
  (http/put (str "/api/playlist/" id)))

(defn playlist-remove! [id]
  (http/delete (str "/api/playlist/" id)))

(defn get-library! []
  (go (let [response (<! (http/get "/api/library"
                                   {:query-params {"q" (@app-state :query)}}))]
        (update-state! :library (get-in response [:body :tracks])))))

(defn upload! []
 ;; TODO: do not use element id
 (let [form (.getElementById js/document
             "upload-form")]
  (update-state! :upload-status "Uploading...")
  ;; TODO: progress bar
  (go (let [response (<! (http/post "/api/library"
                          {:body (js/FormData. form)}))]
       (update-state! :upload-status
        "Done! Now wait for the track being transcoded.")))))

(defn polling-callback []
  (get-info!)
  (get-playlist!)
  (get-library!))

(js/setInterval polling-callback 1000)

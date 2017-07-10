(ns airhead-frontend.requests
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-http.client :as http]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [<! chan pipeline pipe]]
            [airhead-frontend.state :refer [app-state update-state!]]
            [goog.string :as gstring]
            [goog.string.format]))

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

(defn add-transducer
  [in xf]
  (let [out (chan)]
    (pipeline 1 out xf in)
    out))

(defn upload! [form]
  (let [uploading?    #(= (% :direction) :upload)
        to-percentage #(-> (% :loaded)
                           (/ (% :total)) (* 100))
        ; TODO: to-key (fn [%] {:percentage %})
        ;       try to refer map from async
        transducer (comp (filter uploading?)
                         (map to-percentage)
                         (map (fn [%] {:percentage %})))

        progress-chan (chan 1 transducer)
        http-chan (http/post "/api/library" {:body (js/FormData. form)
                                             :progress progress-chan})
        http-chan (add-transducer http-chan
                                  (map (fn [%] {:response %})))
        out-chan (chan)]

    (pipe progress-chan out-chan)
    (pipe http-chan out-chan)

    out-chan))

(defn get-updates! []
  (get-info!)
  (get-playlist!)
  (get-library!))

(get-updates!)

(go
  (let [location js/window.location
        host     location.host
        protocol location.protocol
        ws-path (str (if (= protocol "https:")
                       "wss://" "ws://")
                     host "/api/ws")
        {:keys [ws-channel]} (<! (ws-ch ws-path {:format :json-kw}))]
    (loop []
      (let [{:keys [message]} (<! ws-channel)]
        ;; TODO: parse message
        (get-updates!)
        (when message (recur))))))

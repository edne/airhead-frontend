(ns airhead-cljs.prod
  (:require [airhead-cljs.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)

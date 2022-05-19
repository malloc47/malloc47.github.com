(ns www.server
  (:require
   [optimus.prime :as optimus]
   [optimus.strategies :refer [serve-live-assets #_serve-frozen-assets]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [ring.server.standalone :as ring-server]
   [stasis.core :as stasis]
   [www.config :refer [config]]
   [www.content :refer [content assets]]
   [www.optimizations :as optimizations]
   [www.process :as process]))

(def app (-> (stasis/serve-pages (comp process/return content))
             (optimus/wrap assets
                           optimizations/all
                           serve-live-assets
                           (:optimus config))
             wrap-content-type
             (wrap-default-charset "utf-8")))

(defn serve
  [opts]
  (ring-server/serve
   app
   (merge
    {:join?         true
     :open-browser? true
     :auto-refresh? true}
    opts)))

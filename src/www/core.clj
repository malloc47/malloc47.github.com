(ns www.core
  (:require
   [ring.middleware.content-type :refer [wrap-content-type]]
   [optimus.prime :as optimus]
   [optimus.optimizations :as optimizations]
   [optimus.strategies :refer [serve-live-assets]]
   [optimus.export :refer [save-assets]]
   [ring.server.standalone :as ring-server]
   [stasis.core :as stasis]
   [www.config :refer [config]]
   [www.content :refer [content fixed-assets]]))

(def app (-> (stasis/serve-pages content)
             (optimus/wrap fixed-assets optimizations/none serve-live-assets)
             wrap-content-type))

(defn serve
  [{:keys [join?] :as opts}]
  (ring-server/serve app
    (merge
     {:join?         true
      :open-browser? true
      :auto-refresh? true}
      opts)))

(defn -main []
  (let [{:keys [public-dest]} config
        fixed-assets (optimizations/none (fixed-assets) {})]
    (stasis/empty-directory! public-dest)
    (save-assets fixed-assets public-dest)
    (stasis/export-pages (content) public-dest {:optimus-assets fixed-assets}))
  (System/exit 0))

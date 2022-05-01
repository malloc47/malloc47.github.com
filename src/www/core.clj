(ns www.core
  (:require
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [optimus.prime :as optimus]
   [optimus.strategies :refer [serve-live-assets
                               serve-frozen-assets]]
   [optimus.export :refer [save-assets]]
   [ring.server.standalone :as ring-server]
   [stasis.core :as stasis]
   [www.config :refer [config]]
   [www.content :refer [content assets]]
   [www.optimizations :as optimizations]))

(def app (-> (stasis/serve-pages content)
             (optimus/wrap assets
                           optimizations/all
                           serve-live-assets
                           (:optimus config))
             wrap-content-type
             (wrap-default-charset "utf-8")))

(defn serve
  [{:keys [join?] :as opts}]
  (ring-server/serve app
    (merge
     {:join?         true
      :open-browser? true
      :auto-refresh? true}
      opts)))

(defn -main []
  (let [{:keys [public-dest]} config]
    (stasis/empty-directory! public-dest)
    (as-> (assets) <>
      (optimizations/all <> (:optimus config))
      (remove :bundled <>)
      (remove :outdated <>)
      (save-assets <> public-dest)
      (stasis/export-pages (content) public-dest {:optimus-assets <>})))
  (System/exit 0))

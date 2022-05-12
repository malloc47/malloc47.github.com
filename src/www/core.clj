(ns www.core
  (:require
   [clojure.tools.cli :refer [parse-opts]]
   [optimus.export :refer [save-assets]]
   [optimus.prime :as optimus]
   [optimus.strategies :refer [serve-live-assets #_serve-frozen-assets]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.default-charset :refer [wrap-default-charset]]
   [ring.server.standalone :as ring-server]
   [stasis.core :as stasis]
   [www.config :refer [config]]
   [www.content :refer [content assets]]
   [www.io :as io]
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

(def cli-options
  [["-s" "--spec"]])

(defn -main [& args]
  (println args)
  (let [{{spec? :spec} :options} (parse-opts args cli-options)
        {:keys [public-dest]} config]
    ;; Load for instrumentation side-effects
    (when spec? (println "Instrumenting") (require '[www.spec]))
    (io/write-resources (content))
    (as-> (assets) <>
      (optimizations/all <> (:optimus config))
      (remove :bundled <>)
      (remove :outdated <>)
      (save-assets <> public-dest)))
  (System/exit 0))

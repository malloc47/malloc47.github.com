(ns www.core
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.spec.test.alpha :as stest]
   [clojure.tools.cli :refer [parse-opts]]
   [optimus.export :refer [save-assets]]
   [www.config :refer [config]]
   [www.content :refer [content assets]]
   [www.io :as io]
   [www.optimizations :as optimizations]))

(def cli-options
  [["-s" "--spec"]])

(defn -main [& args]
  (let [{{spec? :spec} :options} (parse-opts args cli-options)
        {:keys [public-dest]} config]
    (when spec?
      (println "Instrumenting")
      (require '[www.spec])
      (spec/check-asserts true)
      (stest/instrument))
    (io/write-resources (:public-dest config) (content))
    (as-> (assets) <>
      (optimizations/all <> (:optimus config))
      (remove :bundled <>)
      (remove :outdated <>)
      (save-assets <> public-dest)))
  (System/exit 0))

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
  [["-s" "--spec"]
   ["-v" "--verbose"]])

(defn print-resource
  [{{input-file :file} :source {output-file :file} :output :as resource}]
  (println (str input-file " -> " output-file))
  resource)

(defn -main [& args]
  (let [{{spec? :spec verbose? :verbose} :options} (parse-opts args cli-options)
        {:keys [public-dest]} config]
    (when spec?
      (println "Instrumenting")
      (require '[www.spec])
      (spec/check-asserts true)
      (stest/instrument))
    (-> (content)
        (cond->> verbose? (map print-resource))
        (->> (io/write-resources (:public-dest config))))
    (as-> (assets) <>
      (optimizations/all <> (:optimus config))
      (remove :bundled <>)
      (remove :outdated <>)
      (save-assets <> public-dest)))
  (System/exit 0))

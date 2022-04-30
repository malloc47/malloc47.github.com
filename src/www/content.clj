(ns www.content
  (:require
   [optimus.assets :refer [load-assets]]
   [stasis.core :refer [slurp-directory merge-page-sources]]
   [www.config :refer [config]]
   [www.process :as process]
   [www.parser :refer [markdown metadata]]))

(defn get-path-contents [path]
  (merge
   (slurp-directory path #"\.md")
   (slurp-directory path #"\.html")))

(defn content []
  (let [root (:content-root config)]
    (->> {:posts   (get-path-contents (str root "/posts/"))
          :pages   (get-path-contents (str root "/pages/"))}
         merge-page-sources
         process/lift
         (map (comp
               process/markdown
               process/metadata
               process/normalize))
         process/return)))

(defn fixed-assets []
  (load-assets "content" [#"\.pdf"]))

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
               process/template
               process/markdown
               process/normalize
               process/metadata))
         process/return)))

(defn fixed-assets []
  (concat
   (load-assets "content" [#"\.pdf"])
   (load-assets "content" [#"\.png" #"\.svg" #"\.jpg"])
   (load-assets "theme" [#"\.woff2"])))

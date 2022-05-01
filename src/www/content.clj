(ns www.content
  (:require
   [optimus.assets :refer [load-assets load-bundle]]
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

(defn assets []
  (concat
   (load-assets "content" [#"\.pdf"])
   (load-assets "content" [#"\.png" #"\.svg" #"\.jpg"])
   (load-assets "content" ["/favicon.ico"])
   (load-assets "theme" [#"\.woff2"])
   (load-bundle "theme"
                "style.css"
                ["/css/reset.css"
                 "/css/style.css"])
   (load-bundle "theme"
                "day.css"
                ["/css/day.css"
                 "/css/img-day.css"
                 "/css/pygments-day.css"])
   (load-bundle "theme"
                "night.css"
                ["/css/night.css"
                 "/css/img-night.css"
                 "/css/pygments-night.css"])
   (load-bundle "theme"
                "main.js"
                ["/js/cookies.js"
                 "/js/mini-clock.js"
                 "/js/check-time.js"
                 "/js/event-handler.js"])))

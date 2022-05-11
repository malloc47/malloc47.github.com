(ns www.content
  (:require
   [optimus.assets :refer [load-assets load-bundle]]
   [www.config :refer [config]]
   [www.io :as io]
   [www.process :as process]))

(defn get-path-contents [path]
  (->> (concat
        (io/read-files path #"\.md")
        (io/read-files path #"\.html"))))

(defn posts []
  (get-path-contents (str (:content-root config) "/posts/")))

(defn pages []
  (get-path-contents (str (:content-root config) "/pages/")))

(defn processed-posts []
  (->> (posts)
       process/add-modified
       process/parse
       process/remove-drafts
       (map process/markdown)
       (sort-by :date)
       reverse))

(defn home+posts []
  (->> (processed-posts)
       (process/template-nested-paginated :home :posts 5)))

(defn feed [layout]
  (let [posts           (processed-posts)
        latest-modified (->> posts (map :modified) sort reverse first)]
    (process/template-nested layout :posts
                             {:latest-modified latest-modified
                              :uri (str "/" (name layout))}
                             posts)))

(defn content []
  (->> (concat (process/run (posts))
               (process/run (pages))
               (home+posts)
               [(feed :rss.xml)
                (feed :atom.xml)])
       process/verify
       process/return))

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

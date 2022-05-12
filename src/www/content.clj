(ns www.content
  (:require
   [optimus.assets :refer [load-bundle]]
   [www.config :refer [config]]
   [www.io :as io]
   [www.process :as process]))

(defn get-path-contents [path]
  (->> (concat
        (io/read-files path #"\.md")
        (io/read-files path #"\.html"))))

(defn content-path
  [path]
  (str (:content-root config) path))

(defn posts []
  (->> "/posts/" content-path get-path-contents))

(defn pages []
  (->> "/pages/" content-path get-path-contents))

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
               (mapcat (fn [regex]
                         (->> regex
                              (io/read-files (content-path "/"))
                              process/run))
                       [#"\.ico" #"\.pdf" #"\.png" #"\.svg" #"\.jpg"])
               (process/run (io/read-files (str "theme")  #"\.woff2"))
               [(feed :rss.xml)
                (feed :atom.xml)])
       process/verify))

(defn assets []
  (concat
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

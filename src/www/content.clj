(ns www.content
  (:require
   [optimus.assets :refer [load-bundle]]
   [www.config :refer [config content-path]]
   [www.io :as io]
   [www.process :as process]))

(def site (select-keys config [:site]))

(defn posts []
  (-> "/posts/" content-path (io/read-files :matcher io/text-types-regex)))

(defn pages []
  (-> "/pages/" content-path (io/read-files :matcher io/text-types-regex)))

(defn processed-posts []
  (->> (posts)
       (process/processed-resources)
       (process/reverse-chronological-sort)))

(defn home+posts []
  (->> (processed-posts)
       (process/template-paginated :home 5 site)))

(defn feed [layout]
  (let [posts           (processed-posts)
        latest-modified (->> posts
                             (map (comp :modified :source))
                             sort
                             reverse
                             first)]
    (process/template-directly
     layout (str "/" (name layout)) posts
     (merge {:latest-modified latest-modified} site))))

(defn content []
  (->> (concat (process/standalone-resources (posts) site)
               (process/standalone-resources (pages) site)
               (home+posts)
               (-> (content-path "/")
                   (io/read-files :matcher
                                  #"\.ico$|\.pdf$|\.png$|\.svg$|\.jpg$")
                   process/copy)
               (-> (io/read-files (str "theme") :matcher #"\.woff2") process/copy)
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

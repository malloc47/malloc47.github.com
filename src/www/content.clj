(ns www.content
  (:require
   [clojure.string :as str]
   [optimus.assets :refer [load-assets load-bundle]]
   [stasis.core :refer [slurp-directory merge-page-sources]]
   [www.config :refer [config]]
   [www.process :as process]))

(defn get-path-contents [path]
  (->> (merge
        (slurp-directory path #"\.md")
        (slurp-directory path #"\.html"))
       process/lift
       (map (fn [{:keys [filename] :as m}]
              (assoc m :full-path
                     (str (str/replace path #"/$" "") filename))))))

(defn posts []
  (get-path-contents (str (:content-root config) "/posts/")))

(defn pages []
  (get-path-contents (str (:content-root config) "/pages/")))

(defn processed-posts []
  (->> (posts)
       process/parse
       process/remove-drafts
       (map process/markdown)
       process/add-modified
       (sort-by :date)
       reverse))

(defn home+posts []
  (->> (processed-posts)
       (process/template-nested-paginated :home :posts 5)
       process/return))

(defn rss []
  (let [posts           (processed-posts)
        latest-modified (->> posts (map :modified) sort reverse first)]
    (->> posts
         (process/template-nested :rss.xml :posts
                                  {:latest-modified latest-modified})
         :content
         (hash-map "/rss.xml"))))

(defn content []
  (->> {:posts (process/run (posts))
        :pages (process/run (pages))
        :home  (home+posts)
        :rss   (rss)}
       merge-page-sources))

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

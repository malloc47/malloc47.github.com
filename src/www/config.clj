(ns www.config)

(def config
  {:public-dest  "public"
   :content-root "content"
   :site-title   "malloc47"
   :site-url     "https://www.malloc47.com"
   :optimus      {:bundle-url-prefix "/assets"
                  :browsers ["> 0.1%, not IE 11"]}})

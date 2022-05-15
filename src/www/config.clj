(ns www.config)

(def config
  {:public-dest  "public"
   :content-root "content"
   :parseable    #{:md :html}
   :site         {:title       "malloc47"
                  :url         "https://www.malloc47.com"
                  :author      "Jarrell Waggoner"
                  :email       "malloc47@gmail.com"
                  :description (str "Blog of Jarrell Waggoner, "
                                    "computer scientist and software "
                                    "developer")
                  :time        (java.util.Date.)}
   :optimus      {:bundle-url-prefix "/assets"
                  :browsers          ["> 0.1%, not IE 11"]}})

(defn content-path
  [path]
  (str (:content-root config) path))

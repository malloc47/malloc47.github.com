(ns www.process
  (:require
   [clojure.string :as str]
   [www.config :refer [config]]
   [www.io :as io]
   [www.parser :as parser]
   [www.render :as renderer])
  (:import
   (java.time LocalDate)))

(defn add-modified
  "Lookup the filename and fetch the last modified timestamp according
  to git, attaching it to the payload."
  [files]
  (map (fn [{:keys [path] :as m}]
         (->> path
              io/most-recent-commit-timestamp
              (assoc m :modified)))
       files))

(defn metadata
  [{:keys [content format] :as m}]
  (if ((:parseable config) format)
    (->> content
         parser/metadata
         (merge m))
    m))

;; Jekyll-style filenaming conventions
(def filename-regexp #"([0-9]{4}-[0-9]{2}-[0-9]{2})-([0-9\w\-]+)\.(\w+)$")

(defn title-ize
  [title]
  (->> (str/split title #"-")
       (map str/capitalize)
       (str/join " ")))

(defn metadata-from-filename
  "extracts metadata-like info from the filename"
  [{:keys [filename relative-path format]}]
  (let [post-name (or (get (re-find filename-regexp filename) 2)
                      (get (re-matches #"(.+)(\.\w+)$" filename) 1))
        relative-path (str/replace-first relative-path filename "")]
    {:title  (some-> post-name title-ize)
     :date   (re-find #"^[0-9]{4}-[0-9]{2}-[0-9]{2}" filename)
     ;; by default, parse-able things have their uri normalized
     ;; without an extension
     :uri    (->> (or (when ((:parseable config) format)
                        post-name)
                      filename)
                  (str relative-path))}))

(defn file->resource
  [{:keys [metadata] :as payload}]
  (-> (merge-with #(some identity %&)
                  payload
                  metadata
                  (metadata-from-filename payload)
                  {:format :unknown})
      (update :uri (comp io/trailing-slash io/leading-slash))
      (update :date #(some-> % (subs 0 10) LocalDate/parse))
      (dissoc :metadata)))

(defn markdown
  [{:keys [format] :as m}]
  (cond-> m
    ((:parseable config) format)
    (update :content parser/markdown)))

(defn template
  [{:keys [layout] :as payload}]
  (cond-> payload
    layout
    (->> (merge (select-keys config [:site]))
         (renderer/template layout)
         constantly
         (update payload :content))))

(defn return
  [resources]
  (->> resources
       (map (fn [{:keys [uri content]}] [uri content]))
       (into {})))

(defn parse
  [resources]
  (map (comp file->resource metadata) resources))

(defn render
  [resources]
  (map (comp template markdown) resources))

(defn remove-drafts
  [resources]
  (remove :draft? resources))

(defn explode-redirects
  [resources]
  (mapcat
   (fn [{:keys [redirects uri] :as m}]
     (->> (or redirects [])
          (map (fn [redirect]
                 (-> m
                     (assoc :uri         redirect
                            :layout      :redirect
                            :destination uri
                            :redirect?   true)
                     template)))
          (concat [m])))
   resources))

(defn verify
  [resources]
  (let [uris (map :uri resources)]
    (when-not (apply distinct? uris)
      (throw (IllegalStateException.
              (str "URIs are not all distinct: "
                   (as-> uris <>
                     (frequencies <>)
                     (for [[uri cnt] <> :when (> cnt 1)] uri)
                     (str/join ", " <>)))))))
  resources)

(defn run
  ([files] (run files {}))
  ([files context]
   (->> files
        parse
        remove-drafts
        (map (fn [m] (merge m context)))
        render
        explode-redirects)))

(defn template-nested
  [layout context-key extra-context nested]
  (->> (hash-map context-key nested)
       (merge extra-context)
       (merge (select-keys config [:site]))
       (merge {:layout layout})
       template))

(defn template-nested-paginated
  ([layout context-key n-per-page nested]
   ;; Sequence of /, page2, ... to mirror Jekyll's paginator
   (let [uri-seq (->> (range)
                      (map (comp #(str "/page" % "/") inc))
                      (replace {"/page1/" "/"}))]
     (template-nested-paginated
      layout context-key n-per-page uri-seq nested)))
  ([layout context-key n-per-page uri-seq nested]
   (let [nest-groups (->> nested
                          (sort-by :date)
                          reverse
                          (partition-all n-per-page))]
     (->> nest-groups
          (map (fn [[prev-uri uri next-uri] nested]
                 (->> (hash-map context-key nested)
                      (merge {:layout   layout
                              ;; TODO: abstract title generation
                              :title    (subs uri 1)
                              :uri      uri
                              :next-uri next-uri
                              :prev-uri prev-uri})
                      template))
               ;; Offset to give access to prev/next URIs
               (as-> uri-seq <>
                 (take (count nest-groups) <>)
                 (concat [nil] <> [nil])
                 (partition 3 1 <>)))))))

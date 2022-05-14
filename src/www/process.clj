(ns www.process
  (:require
   [clojure.instant :refer [read-instant-date]]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [www.config :refer [config]]
   [www.io :as io]
   [www.parser :as parser]
   [www.render :as renderer]))

(defn add-modified
  "Lookup the filename and fetch the last modified timestamp according
  to git, attaching it to the payload."
  [files]
  (map (fn [{:keys [path] :as m}]
         (->> path
              io/most-recent-commit-timestamp
              (assoc m :modified)))
       files))

(defn metadata-from-header
  [{:keys [content] {:keys [format]} :source :as m}]
  (if ((:parseable config) format)
    (let [{{:keys [layout uri] :as metadata} :metadata
           content                           :content}
          (->> content
               parser/metadata)]
      (s/assert :resource/metadata-header metadata)
      (-> m
          (assoc :content content)
          (update :metadata #(merge % (dissoc metadata :layout :uri)))
          (conj (when layout [:template {:layout layout}]))
          (conj (when uri [:uri (io/slash uri)]))))
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
  [{:keys [uri] {:keys [filename format]} :source :as m}]
  (let [post-name     (or (get (re-find filename-regexp filename) 2)
                          (get (re-matches #"(.+)(\.\w+)$" filename) 1))
        ;; assumes that the relative path was dumped into the URI
        relative-path (str/replace-first uri filename "")
        metadata      {:title  (some-> post-name title-ize)
                       :date   (some->> filename
                                        (re-find #"^[0-9]{4}-[0-9]{2}-[0-9]{2}")
                                        read-instant-date)}
        ;; by default, parse-able things have their uri normalized
        ;; without an extension
        uri           (->> (or (when ((:parseable config) format)
                                 post-name)
                               filename)
                           (str relative-path)
                           io/slash)]
    (-> m
        (update :metadata #(merge % metadata))
        (conj (when uri [:uri uri])))))

(defn markdown
  [{{:keys [format]} :source :as m}]
  (cond-> m
    ((:parseable config) format)
    (update :content parser/markdown)))

(defn template
  [{{:keys [layout]} :template :as payload}]
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

(defn remove-drafts
  [resources]
  (remove (comp :draft? :metadata) resources))

(defn explode-redirects
  [resources]
  (mapcat
   (fn [{:keys [uri] {:keys [redirects]} :metadata :as m}]
     (->> (or redirects [])
          (map (fn [redirect]
                 (-> m
                     (assoc :uri         redirect
                            :destination uri)
                     (assoc-in [:template :layout] :redirect)
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

(defn files->processed-resources
  [files]
  (->> files
       (map (comp metadata-from-header metadata-from-filename))
       remove-drafts
       explode-redirects
       (map markdown)))

(defn sort-resources
  [resources]
  (assert (every? (comp :date :metadata) resources)
          "Cannot sort with missing dates!")
  (->> resources
       (sort-by (comp :date :metadata))
       reverse))

;; Templating is separated because there's a number of cases where we
;; want to get untemplated but otherwise completely processed
;; resources to nest inside of higher-level pages.
(defn files->templated-resources
  [files]
  (->> files
       files->processed-resources
       (map template)))

(defn template-nested
  [layout context-key extra-context nested]
  (->> (hash-map context-key nested)
       (merge extra-context)
       (merge {:template {:layout layout}})
       template))

(defn template-nested-paginated
  "Creates a paginated series of pages based on a set of
  resoruces. Assumes that the nested resources are already sorted and
  that each page will have n-per-page number of resources. Can specify
  the uri-seq to control the URIs for the pages, defaults to a
  Jekyll-style paginator with paginate_path: \"page:num\" configured."
  ([layout context-key n-per-page nested]
   ;; Sequence of /, page2, ... to mirror Jekyll's paginator
   (let [uri-seq (->> (range)
                      (map (comp #(str "/page" % "/") inc))
                      (replace {"/page1/" "/"}))]
     (template-nested-paginated
      layout context-key n-per-page uri-seq nested)))
  ([layout context-key n-per-page uri-seq nested]
   (let [nest-groups (partition-all n-per-page nested)]
     (map (fn [[prev-uri uri next-uri] nested]
            (->> (hash-map context-key nested)
                 (merge {:template {:layout  layout}
                         ;; TODO: abstract title generation
                         :metadata {:title (subs uri 1)}
                         :uri      uri
                         :next-uri next-uri
                         :prev-uri prev-uri})
                 template))
          ;; Offset to give access to prev/next URIs
          (as-> uri-seq <>
            (take (count nest-groups) <>)
            (concat [nil] <> [nil])
            (partition 3 1 <>))
          nest-groups))))

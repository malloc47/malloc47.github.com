(ns www.process
  (:require
   [clojure.instant :refer [read-instant-date]]
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [www.config :refer [config]]
   [www.io :refer [slash]]
   [www.parser :as parser]
   [www.render :as renderer]))

(defn metadata-from-header
  [{:keys [content] {:keys [header?]} :source :as m}]
  (if header?
    (let [{{:keys [layout uri] :as metadata} :metadata
           content                           :content}
          (parser/metadata content)]
      (s/assert :resource/metadata-header metadata)
      (-> m
          (assoc :content content)
          (update :metadata #(merge % (dissoc metadata :layout :uri)))
          (conj (when layout [:template {:layout layout}]))
          (conj (when uri [:uri (slash uri)]))))
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
        ;; TODO: decomplect this
        ;; by default, parse-able things have their uri normalized
        ;; without an extension
        uri           (->> (or (when ((:parseable config) format)
                                 post-name)
                               filename)
                           (str relative-path)
                           slash)]
    (-> m
        (update :metadata #(merge % metadata))
        (conj (when uri [:uri uri])))))

(defmulti parse (comp :format :source))

(defmethod parse :md [m]
  (update m :content parser/markdown))

(defmethod parse :default [m] m)

(defn template
  [{{:keys [layout]} :template :as payload}]
  (if layout
    (let [rendered (->> payload
                        (merge (select-keys config [:site]))
                        (renderer/template layout))]
      (-> payload
          (assoc :content rendered)
          (assoc-in [:template :file]
                    (-> layout renderer/template-file io/file))))
    ;; else do nothing
    payload))

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

(defn output-location
  [{:keys [uri] :as m}]
  (let [path (str (:public-dest config) uri)
        file (io/file
              (cond-> path
                (str/ends-with? path "/")
                (str "index.html")))]
    (assoc m :output {:file file})))

;;; High-level functions for building collections of resources (pages)
;;; from sources (files).

(defn processed-resources
  "Parse, and render a collection of file sources. Does not template,
  so this helper function is useful for generating resources that are
  passed along as a nested resources of another templated page. Does
  not do redirect expansion."
  [sources]
  (->> sources
       (map (comp metadata-from-header metadata-from-filename))
       remove-drafts
       (map parse)))

(defn reverse-chronological-sort
  [resources]
  (->> resources
       (sort-by (comp :date :metadata))
       reverse))

(defn standalone-resources
  "Completely parse, render, and template a collection of file
  sources. This processes file sources 1:1 with templated pages."
  [sources]
  (->> sources
       (map (comp metadata-from-header metadata-from-filename))
       remove-drafts
       explode-redirects
       (map (comp output-location template parse))))

(defn copy
  "Asumes source is not parseable and do the minimal amount of work to
  extract the URI from the filename; when exported later, the file
  contents will be entirely untransformed (i.e. clojure.java.io/copy)."
  [sources]
  (map (comp output-location metadata-from-filename) sources))

(defn template-directly
  "Create and template a new resource without having ingested the
  content from a specific file source."
  [layout uri content & {:as extra}]
  (-> {:uri uri :content content :template {:layout layout}}
      (merge extra)
      template
      output-location))

(defn template-paginated
  "Creates a paginated series of pages based on a set of 'nested'
  resources. Assumes that the nested resources are already sorted and
  that each resulting page will have n-per-page number of
  resources. Can specify the uri-seq to control the URIs for the
  pages, defaults to a Jekyll-style paginator with paginate_path:
  \"page:num\" configured."
  ([layout n-per-page nested]
   ;; Sequence of /, page2, ... to mirror Jekyll's paginator
   (let [uri-seq (->> (range)
                      (map (comp #(str "/page" % "/") inc))
                      (replace {"/page1/" "/"}))]
     (template-paginated layout n-per-page uri-seq nested)))
  ([layout n-per-page uri-seq nested]
   (let [nest-groups (partition-all n-per-page nested)]
     (map (fn [[prev-uri uri next-uri] nested]
            (-> {:content  nested
                 :uri      uri
                 :next-uri next-uri
                 :prev-uri prev-uri
                 :template {:layout  layout}
                 ;; TODO: abstract title generation
                 :metadata {:title (subs uri 1)}}
                template
                output-location))
          ;; Offset to give access to prev/next URIs
          (as-> uri-seq <>
            (take (count nest-groups) <>)
            (concat [nil] <> [nil])
            (partition 3 1 <>))
          nest-groups))))

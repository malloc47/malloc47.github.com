(ns www.process
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [selmer.filters :refer [add-filter!]]
   [selmer.parser :as selmer]
   [www.config :refer [config]]
   [www.parser :as parser])
  (:import
   (java.time LocalDate)
   (java.net URLEncoder)
   (java.util TimeZone)
   (java.text SimpleDateFormat)
   (org.apache.commons.text StringEscapeUtils)))

(defn metadata
  [{:keys [content] :as m}]
  (->> content
       parser/metadata
       (merge m)))

(def file-extension-regexp
  #"\.(\w+)$")

(defn trailing-slash
  [s]
  (cond-> s
    (and (not (str/ends-with? s "/"))
         ;; does not have file extension
         (not (get (re-find file-extension-regexp s) 1)))
    (str "/")))

(defn leading-slash
  [s]
  (cond->> s (not (str/starts-with? s "/")) (str "/")))

;; Jekyll-style filenaming conventions
(def filename-regexp #"/([0-9]{4}-[0-9]{2}-[0-9]{2})-([0-9\w\-]+)\.(\w+)$")

(defn normalize
  [{{:keys [permalink date format layout title draft? redirects]} :metadata
    filename :filename :as m}]
  (let [file-date  (re-find #"^[0-9]{4}-[0-9]{2}-[0-9]{2}" filename)
        file-title (get (re-find filename-regexp filename) 2)
        format     (keyword (or format
                                (get (re-find #"\.(\w+)$" filename) 1)
                                :unknown))
        uri        (-> (or permalink
                           file-title
                           ;; Pages as opposed to posts don't have a
                           ;; date, so fall back to getting the
                           ;; filename without it
                           (get (re-matches #"(.+)(\.\w+)$" filename) 1)
                           filename)
                       trailing-slash
                       leading-slash)
        date       (some-> (or date file-date) (subs 0 10) LocalDate/parse)
        title      (or title file-title)]
    (-> {:format    format
         :uri       uri
         :date      date
         :layout    layout
         :title     title
         :draft?    draft?
         :permalink permalink
         :redirects redirects}
        (->> (merge m))
        (dissoc :metadata))))

(defn markdown
  [{:keys [format] :as m}]
  (cond-> m
    (= format :md)
    (update :content parser/markdown)))

(selmer/set-resource-path! (io/resource "META-INF/theme"))
(add-filter! :uricomp_encode #(URLEncoder/encode % "UTF-8"))
(add-filter! :xml_escape (fn [s] (StringEscapeUtils/escapeXml10 s)))
(add-filter! :rfc822_date
             (fn [date]
               (-> (doto (SimpleDateFormat.
                          "EEE, dd MMM yyyy HH:mm:ss zzz")
                     (.setTimeZone
                      (TimeZone/getTimeZone "GMT")))
                   (.format date))))
(add-filter! :iso8601_date
             (fn [date]
               (-> (doto (SimpleDateFormat.
                          "yyyy-MM-dd'T'HH:mm:ss'Z'")
                     (.setTimeZone
                      (TimeZone/getTimeZone "UTC")))
                   (.format date))))

(defn template
  [{:keys [layout] :as payload}]
  (cond-> payload
    layout
    (->> (merge (select-keys config [:site]))
         (selmer/render-file (cond-> (str "layouts/" (name layout))
                               (-> (re-find file-extension-regexp (name layout))
                                   (get 1)
                                   not)
                               (str ".html")))
         constantly
         (update payload :content))))

;; map/list processing functions

(defn lift
  "convert single map of paths->contents into individual maps."
  [m]
  (map (fn [[k v]] {:content v :filename k}) m))

(defn return
  [contents]
  (->> contents
       (map (fn [{:keys [uri content]}] [uri content]))
       (into {})))

(defn parse
  [contents]
  (map (comp normalize metadata) contents))

(defn render
  [contents]
  (map (comp template markdown) contents))

(defn remove-drafts
  [contents]
  (remove :draft? contents))

(defn explode-redirects
  [contents]
  (mapcat
   (fn [{:keys [redirects permalink] :as c}]
     (->> (or redirects [])
          (map (fn [redirect]
                 (-> c
                     (assoc :uri         redirect
                            :layout      :redirect
                            :destination permalink
                            :redirect?   true)
                     template)))
          (concat [c])))
   contents))

(defn run
  ([contents] (run contents {}))
  ([contents context]
   (->> contents
        parse
        remove-drafts
        (map (fn [c] (merge c context)))
        render
        explode-redirects
        return)))

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
                      (merge {:layout layout
                              ;; TODO: abstract title generation
                              :title (subs uri 1)
                              :uri uri
                              :next-uri next-uri
                              :prev-uri prev-uri})
                      template))
               ;; Offset to give access to prev/next URIs
               (as-> uri-seq <>
                 (take (count nest-groups) <>)
                 (concat [nil] <> [nil])
                 (partition 3 1 <>)))))))

(defn add-modified
  "Lookup the filename and fetch the last modified timestamp according
  to git, attaching it to the payload."
  [contents]
  (map (fn [{:keys [full-path] :as c}]
         (let [git-timestamp
               (->> full-path
                    (sh "git" "log" "-1" "--pretty=%ct")
                    :out
                    str/trim)]
           (try
             (->> git-timestamp
                  (Long.)
                  (* 1000)
                  (java.util.Date.)
                  (assoc c :modified))
             (catch NumberFormatException _
               ;; TODO: make the default selectable?
               (assoc c :modified (java.util.Date.))))))
       contents))
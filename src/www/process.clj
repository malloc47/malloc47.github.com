(ns www.process
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [selmer.parser :as selmer]
   [www.config :refer [config]]
   [www.parser :as parser])
  (:import
   (java.time LocalDate)))

(defn metadata
  [{:keys [content] :as m}]
  (->> content
       parser/metadata
       (merge m)))

(defn trailing-slash
  [s]
  (cond-> s
    (and (not (str/ends-with? s "/"))
         ;; does not have file extension
         (not (get (re-find #"\.(\w+)$" s) 1)))
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

(defn template
  [{:keys [layout] :as payload}]
  (cond-> payload
    layout (->>
            (merge config)
            (selmer/render-file (str "html/" (name layout) ".html"))
            constantly
            (update payload :content))))

;; Map/list processing functions

(defn lift
  "Convert single map of paths->contents into individual maps."
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
        lift
        parse
        remove-drafts
        (map (fn [c] (merge c context)))
        render
        explode-redirects
        return)))

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

(ns www.process
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [www.config :refer [config]]
   [www.parser :as parser]
   [www.render :as renderer])
  (:import
   (java.time LocalDate)))

(defn metadata
  [{:keys [content] :as m}]
  (->> content
       parser/metadata
       (merge m)))

(s/fdef metadata
  :args (s/cat :input :resource/lifted)
  :ret :resource/split)

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
         :layout    layout
         :title     title}
        (cond->
         draft?    (assoc :draft? draft?)
         redirects (assoc :redirects redirects)
         date      (assoc :date date))
        (->> (merge m))
        (dissoc :metadata))))

(s/fdef normalize
  :args (s/cat :input :resource/split)
  :ret :resource/payload)

(defn markdown
  [{:keys [format] :as m}]
  (cond-> m
    (= format :md)
    (update :content parser/markdown)))

(s/fdef markdown
  :args (s/cat :input (s/keys :req-un [:resource/format :resource/content]))
  :ret :resource/payload)

(defn template
  [{:keys [layout] :as payload}]
  (cond-> payload
    layout
    (->> (merge (select-keys config [:site]))
         (renderer/template layout)
         constantly
         (update payload :content))))

(s/fdef template
  :args (s/cat :input (s/keys :req-un [:resource/layout]))
  :ret :resource/payload)

;; map/list processing functions

(defn lift
  "convert single map of paths->contents into individual maps."
  [m]
  (map (fn [[k v]] {:content v :filename k}) m))

(s/fdef lift
  :args (s/cat :input :resource/raw)
  :ret (s/coll-of :resource/payload))

(defn return
  [contents]
  (->> contents
       (map (fn [{:keys [uri content]}] [uri content]))
       (into {})))

(s/fdef return
  :args (s/cat :input (s/coll-of :resource/payload))
  :ret (s/coll-of :resource/raw))

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
   (fn [{:keys [redirects uri] :as c}]
     (->> (or redirects [])
          (map (fn [redirect]
                 (-> c
                     (assoc :uri         redirect
                            :layout      :redirect
                            :destination uri
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

(comment
  (require '[clojure.spec.test.alpha :as stest])
  (stest/instrument
   [`metadata
    `normalize
    `markdown
    `template
    `lift
    `return]))

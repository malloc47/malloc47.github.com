(ns www.process
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [selmer.parser :as selmer]
   [www.config :refer [config]]
   [www.parser :as parser])
  (:import
   (java.time LocalDate)))

(defn lift
  "Convert single map of paths->contents into individual maps."
  [m]
  (map (fn [[k v]] {:content v :filename k}) m))

(defn metadata
  [{:keys [content] :as m}]
  (->> content
       parser/metadata
       (merge m)))

(defn trailing-slash
  [s]
  (cond-> s (not (str/ends-with? s "/")) (str "/")))

(defn normalize
  [{{:keys [permalink date format layout title]} :metadata
    filename :filename :as m}]
  (let [format (keyword (or format
                            (get (re-find #"\.(\w+)$" filename) 1)
                            :unknown))
        uri    (trailing-slash
                (or permalink
                    (get (re-matches #"(.+)(\.\w+)$" filename) 1)
                    filename))
        date   (some-> (or date
                           (re-find #"^[0-9]{4}-[0-9]{2}-[0-9]{2}" filename))
                       (subs 0 10)
                       LocalDate/parse)
        title  (or title
                   (get (re-find #"[0-9]{4}-[0-9]{2}-[0-9]{2}-([0-9\w\-]+)\.md" filename) 1))]
    (-> {:format format
         :uri    uri
         :date   date
         :layout layout
         :title  title}
        (->> (merge m))
        (dissoc :metadata))))

(defn markdown
  [{:keys [format] :as m}]
  (cond-> m
    (= format :md)
    (update :content parser/markdown)))

(selmer/set-resource-path! (io/resource "META-INF/theme"))

(defn template
  [{:keys [layout content] :as payload}]
  (cond-> payload
    layout (->>
            (merge config)
            (selmer/render-file (str "html/" (name layout) ".html"))
            constantly
            (update payload :content))))

(defn return
  [ms]
  (->> ms
       (map (fn [{:keys [uri content]}] [uri content]))
       (into {})))

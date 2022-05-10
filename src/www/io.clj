(ns www.io
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str])
  (:import
   (java.util Date)))

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

(defn read-files
  [path matcher & opts]
  (->> (io/file path)
       file-seq
       (filter (every-pred #(.isFile %)
                           #(re-find matcher (str %))))
       (map (fn [f]
              (let [file-path (str f)
                    filename  (.getName f)]
                {:path     file-path
                 ;; Since downstream utilities will not know what path
                 ;; was specified, they need to know the relative path
                 ;; following the given path to later construct a
                 ;; proper URI.
                 :relative-path (-> file-path
                                    (str/replace-first path "")
                                    leading-slash)
                 :filename filename
                 :content  (apply slurp f opts)
                 :format   (-> (re-find #"\.(\w+)$" filename)
                               ;; first capture group
                               (get 1)
                               keyword)})))))

(defn most-recent-commit-timestamp
  [path]
  (let [git-timestamp
        (->> path
             (sh "git" "log" "-1" "--pretty=%ct")
             :out
             str/trim)]
    (try
      (->> git-timestamp
           ;; TODO: use a real date library
           (Long.)
           (* 1000)
           (Date.))
      (catch NumberFormatException _
        ;; TODO: make the default selectable?
        (Date.)))))

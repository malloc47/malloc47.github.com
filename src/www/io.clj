(ns www.io
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [www.config :refer [config]])
  (:import
   (java.util Date)
   (java.io File)
   (org.apache.commons.io FileUtils)))

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
  ([path] (read-files path nil))
  ([path matcher & opts]
   (->> (io/file path)
        file-seq
        (filter (every-pred #(.isFile %)
                            (fn [f] (if matcher
                                      (->> f str (re-find matcher))
                                      true))))
        (map (fn [f]
               (let [file-path (str f)
                     filename  (.getName f)
                     format    (-> (re-find #"\.(\w+)$" filename)
                                   ;; first capture group
                                   (get 1)
                                   keyword)]
                 {:path          file-path
                  ;; Since downstream utilities will not know what path
                  ;; was specified, they need to know the relative path
                  ;; following the given path to later construct a
                  ;; proper URI.
                  :relative-path (-> file-path
                                     (str/replace-first path "")
                                     leading-slash)
                  :filename      filename
                  :format        format
                  :content       (if ((:parseable config) format)
                                   (apply slurp f opts)
                                   f)}))))))

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

(defn delete-directory!
  [path]
  (-> path io/file FileUtils/deleteDirectory))

(defn write
  [output-file content]
  (cond
    (string? content) (spit output-file content)
    (instance? File content) (io/copy content output-file)))

(defn write-file
  [uri content]
  (let [path (str (:public-dest config) uri)
        file (io/file
              (cond-> path
                (str/ends-with? path "/")
                (str "index.html")))]
    (-> file .getParentFile .mkdirs)
    (write file content)))

(defn write-resources
  [resources]
  (delete-directory! (:public-dest config))
  (doseq [{:keys [content uri]} resources]
    (write-file uri content)))

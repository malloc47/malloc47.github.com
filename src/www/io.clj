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

(def slash
  (comp trailing-slash leading-slash))

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

(defn file->source
  [path opts f]
  (let [file-path     (str f)
        filename      (.getName f)
        format        (-> (re-find #"\.(\w+)$" filename)
                          ;; first capture group
                          (get 1)
                          keyword)
        relative-path (-> file-path
                          (str/replace-first path "")
                          leading-slash)]
    {:source  {:path     file-path
               :filename filename
               :format   format
               :modified (-> file-path
                             most-recent-commit-timestamp)}
     :content (if ((:parseable config) format)
                (apply slurp f opts)
                f)
     :uri     relative-path}))

(defn read-files
  ([path] (read-files path nil))
  ([path matcher & opts]
   (->> (io/file path)
        file-seq
        (filter (every-pred #(.isFile %)
                            (fn [f] (if matcher
                                      (->> f str (re-find matcher))
                                      true))))
        (map (partial file->source path opts)))))

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

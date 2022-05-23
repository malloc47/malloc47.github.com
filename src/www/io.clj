(ns www.io
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str])
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

(defn git-commit-selection-command
  [path]
  ;; You'd really think this could be done natively by git log, but
  ;; the --follow + --grep + --invert-grep commands do not play nicely
  ;; https://stackoverflow.com/a/64468571
  ["/usr/bin/env" "bash" "-c"
   (str "git log --follow -M --pretty='%f %ct' --  "
        path
        " | grep -v NONCONTENT | head -1 | awk '{print $2}'")])

(defn most-recent-commit-timestamp
  "Looks up a given path in git to find the latest commit, extracting
  the timestamp representing when it was last modified. Skips over
  commits that have \"NOCONTENT\" in the subject line of the commit
  message."
  [path]
  (let [git-timestamp
        (->> path
             (git-commit-selection-command)
             (apply sh)
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

(defn maybe-bool-to-fn
  [v]
  (if (ifn? v)
    v
    (constantly (boolean v))))

(defn file->source
  "Converts a root path and file to a source map. Optional args:

  - header? : Determines whether the file is expected to have an EDN
  header, can be either a function that takes the string content as an
  argument and returns a boolean, or a plain boolean value. Defaults
  to false.

  - binary? : Specifies the file should be not slurped as a string,
  can be either a function that takes file extension as a keyword and
  returns a boolean, or a plain boolean value. Defaults to false.

  All other args will be forwarded to slurp."
  [path f & {:keys [header? binary?] :as opts}]
  (let [file-path     (str f)
        filename      (.getName f)
        format        (-> (re-find #"\.(\w+)$" filename)
                          ;; first capture group
                          (get 1)
                          keyword)
        relative-path (-> file-path
                          (str/replace-first path "")
                          leading-slash)
        header?       (maybe-bool-to-fn header?)
        binary?       (maybe-bool-to-fn binary?)
        content       (if-not (binary? format)
                        (->> (dissoc opts :header? :binary?)
                             (apply concat)
                             (apply slurp f))
                        f)]
    {:source  {:file     f
               :filename filename
               :format   format
               :modified (-> file-path
                             most-recent-commit-timestamp)
               :header? (header? content)}
     :content content
     :uri     relative-path}))

(def text-types
  #{:md :html :yaml})

(def text-types-regex
  (->> text-types
       (map #(str "\\." (name %) "$"))
       (str/join "|")
       re-pattern))

(def default-bulk-read-opts
  {:header? (fn [content]
              (and (string? content)
                   (str/starts-with? content "{")))
   :binary? (comp not text-types)})

(defn read-files
  [path & {:keys [matcher] :as opts}]
  (let [opts (merge default-bulk-read-opts opts)]
    (->> (io/file path)
         file-seq
         (filter (every-pred #(.isFile %)
                             (fn [f] (if matcher
                                       (->> f str (re-find matcher))
                                       true))))
         (map #(file->source path % (dissoc opts :matcher))))))

(defn delete-directory!
  [path]
  (-> path io/file FileUtils/deleteDirectory))

(defn write-file
  [file content]
  (-> file .getParentFile .mkdirs)
  (cond
    (string? content) (spit file content)
    (instance? File content) (io/copy content file)))

(defn write-resources
  [destination resources]
  (delete-directory! destination)
  (doseq [{:keys [content] {:keys [file]} :output} resources]
    (write-file file content)))

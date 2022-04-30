(ns www.parser
  (:require
   [www.parser.flexmark :as flexmark]
   [clojure.java.io :refer [copy]]
   [selmer.parser :refer [render render-file]]
   [clojure.string :as str])
  (:import
   (java.io StringReader StringWriter PushbackReader)))

(defn metadata
  [content]
  ;; Use the initial character to determine if there's a header
  (if (str/starts-with? content "{")
    ;; This is unplesantly complicated just to read out the edn header
    (with-open [reader   (-> content StringReader. PushbackReader.)
                writer   (StringWriter.)]
      (let [metadata (read reader)]
        (copy reader writer)
        {:metadata metadata
         :content  (.toString writer)}))
    ;; No header case is just the
    {:metadata {}
     :content content}))

(defn markdown
  [markdown-string]
  (->> markdown-string
       (.parse flexmark/parser)
       (.render flexmark/renderer)))

(defn template [untemplated-string]
  (render untemplated-string))

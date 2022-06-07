(ns www.parser
  (:require
   [clojure.java.io :refer [copy]])
  (:import
   (java.io StringReader StringWriter PushbackReader)))

(defn metadata
  [content]
  ;; Use the initial character to determine if there's a header
  (with-open [reader (-> content StringReader. PushbackReader.)
              writer (StringWriter.)]
    (let [metadata (read reader)]
      (copy reader writer)
      {:metadata metadata
       :content  (.toString writer)})))

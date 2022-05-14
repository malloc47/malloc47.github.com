(ns www.render
  (:require
   [clojure.java.io :as io]
   [selmer.filters :refer [add-filter!]]
   [selmer.parser :as selmer])
  (:import
   (java.net URLEncoder)
   (java.util TimeZone)
   (java.text SimpleDateFormat)
   (org.apache.commons.text StringEscapeUtils)))

(selmer/set-resource-path! (io/resource "META-INF/theme"))

;;; These bring selmer closer to alignment with
;;; https://jekyllrb.com/docs/liquid/ filters, namely cgi_escape,
;;; xml_escape, date_to_xmlschema, and date_to_rfc822
(add-filter! :uricomp_encode #(URLEncoder/encode % "UTF-8"))
(add-filter! :xml_escape (fn [s] (StringEscapeUtils/escapeXml10 s)))
;; TODO: use clj-time or another high-level library?
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
  [layout content]
  (selmer/render-file
   (cond-> (str "layouts/" (name layout))
     (-> (re-find #"\.(\w+)$" (name layout))
         (get 1)
         not)
     (str ".html"))
   content))

(ns www.spec
  (:require
   [clojure.spec.alpha :as s]
   [www.io :as io]
   [www.process :as process]
   [www.views :as views])
  (:import
   (java.time LocalDate)
   (java.io File)))

(s/def :general/date
  (s/or :inst inst?
        :local-date (partial instance? LocalDate)))

(s/def :file/path string?)
(s/def :file/relative-path string?)
(s/def :file/filename string?)
(s/def :file/format (s/nilable keyword?))
(s/def :file/modified inst?)

(s/def :metadata/date string?)

(s/def :resource/date (s/nilable :general/date))
(s/def :resource/layout
  (s/or
   :fixed #{:page :post :home :redirect}
   :filename (s/and
              keyword?
              (comp (partial re-matches #"\w+\.\w+$")
                    name))))
(s/def :resource/title string?)
(s/def :resource/draft? boolean?)
(s/def :resource/redirect? boolean?)
(s/def :resource/redirects (s/or :single string?
                                 :multiple (s/coll-of string?)))
(s/def :resource/uri string?)
(s/def :resource/content (s/or :string string?
                               :file (partial instance? File)))
(s/def :site/title string?)
(s/def :site/url string?)
(s/def :site/author string?)
(s/def :site/email string?)
(s/def :site/description string?)
(s/def :site/time inst?)

(s/def :resource/raw
  (s/keys :req-un [:file/path
                   :file/relative-path
                   :file/filename
                   :file/format
                   :resource/content]))

(s/def :resource/payload
  (s/keys :req-un [:resource/layout
                   :resource/title
                   :resource/uri]
          :opt-un [:resource/content
                   :file/filename
                   :file/format
                   :resource/draft?
                   :resource/redirects
                   :file/modified
                   :resource/date]))

(s/def :resource/metadata
  (s/keys :req-un [:resource/title
                   :resource/layout]
          :opt-un [:resource/draft?
                   :resource/redirects
                   :resource/uri
                   :metadata/date]))

(s/def :resource/site
  (s/keys :req-un [:site/title
                   :site/url
                   :site/author
                   :site/email
                   :site/description
                   :site/time]))

(s/def :resource/enriched
  (s/merge :resource/raw
           (s/keys :opt-un [:resource/metadata])))

(s/def :resource/full
  (s/merge :resource/payload (s/keys :req-un [:site/site :resource/site])))

;;; Instrumentation

(s/fdef io/read-files
  :args (s/cat :path string?
               :matcher (partial instance? java.util.regex.Pattern)
               :else (s/* any?))
  :ret :resource/raw)

(s/fdef process/add-modified
  :args (s/cat :input (s/coll-of :resource/raw))
  :ret (s/merge :resource/raw (s/keys :req-un [:file/modified])))

(s/fdef process/metadata
  :args (s/cat :input :resource/raw)
  :ret :resource/enriched)

(s/fdef process/file->resource
  :args (s/cat :input :resource/enriched)
  :ret :resource/payload)

(s/fdef process/markdown
  :args (s/cat :input (s/keys :req-un [:resource/format :resource/content]))
  :ret :resource/payload)

(s/fdef process/template
  :args (s/cat :input (s/keys :req-un [:resource/layout]))
  :ret :resource/payload)

(s/fdef process/explode-redirects
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/uri]
                                         :opt-un [:resource/redirects])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/verify
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/uri])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/return
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/uri
                                                  :resource/content])))
  :ret (s/map-of :resource/uri :resource/content))

(s/fdef views/base
  :args (s/cat :input :resource/full)
  :ret vector?)

(do #_comment
 (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument
     [`io/read-files
      `process/add-modified
      `process/metadata
      `process/file->resource
      `process/markdown
      `process/template
      `process/explode-redirects
      `process/verify
      `process/return
      `views/base]))

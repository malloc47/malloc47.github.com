(ns www.spec
  (:require
   [clojure.spec.alpha :as s]
   [www.io :as io]
   [www.process :as process]
   [www.views :as views])
  (:import
   (java.io File)))

;;; Fields

(s/def :general/date inst?)

(s/def :file/file (partial instance? File))
(s/def :file/filename string?)
(s/def :file/format (s/nilable keyword?))
(s/def :file/modified inst?)

(s/def :metadata/date (s/nilable :general/date))
(s/def :template/layout
  (s/or
   :fixed #{:page :post :home :redirect}
   :filename (s/and
              keyword?
              (comp (partial re-matches #"\w+\.\w+$")
                    name))))
(s/def :metadata/title string?)
(s/def :metadata/draft? boolean?)
(s/def :metadata/redirects (s/or :single string?
                                 :multiple (s/coll-of string?)))
(s/def :resource/uri string?)
(s/def :resource/content (s/or :string string?
                               :file (partial instance? File)
                               :nested coll?))

(s/def :site/title string?)
(s/def :site/url string?)
(s/def :site/author string?)
(s/def :site/email string?)
(s/def :site/description string?)
(s/def :site/time inst?)

;;; Entities

(s/def :resource/source
  (s/keys :req-un [:file/file
                   :file/filename
                   :file/format
                   :file/modified]))

(s/def :resource/base
  (s/keys :req-un [:resource/uri
                   :resource/content]))

(s/def :resource/metadata
  (s/keys :opt-un [:resource/uri
                   :metadata/title
                   :metadata/draft?
                   :metadata/redirects
                   :metadata/date]))

(s/def :resource/template
  (s/keys :req-un [:template/layout]))

(s/def :resource/metadata-header
  (s/merge :resource/metadata
           :resource/template))

(s/def :resource/output
  (s/keys :req-un [:file/file]))

(s/def :resource/payload
  (s/merge :resource/base
           (s/keys :opt-un [:resource/source])
           (s/keys :opt-un [:resource/metadata])
           (s/keys :opt-un [:resource/template])
           (s/keys :opt-un [:resource/output])))

(s/def :resource/site
  (s/keys :req-un [:site/title
                   :site/url
                   :site/author
                   :site/email
                   :site/description
                   :site/time]))

(s/def :resource/full
  (s/merge :resource/payload
           (s/keys :req-un [:resource/site])))

;;; Instrumentation

(s/fdef www.io/read-files
  :args (s/cat :path string?
               :matcher (partial instance? java.util.regex.Pattern)
               :else (s/* any?))
  :ret (s/merge :resource/base (s/keys :req-un [:resource/source])))

(s/fdef www.io/write-resources
  :args (s/cat :input (s/coll-of
                       (s/merge :resource/base
                                (s/keys :req-un [:resource/output])))))

(s/fdef www.io/write-file
  :args (s/cat :file :file/file :content :resource/content))

(s/fdef process/metadata-from-header
  :args (s/cat :input (s/merge :resource/base
                               (s/keys :opt-un [:resource/source])))
  :ret (s/merge :resource/base
                (s/keys :req-un [:resource/source])
                (s/keys :opt-un [:resource/metadata])))

(s/fdef process/metadata-from-filename
  :args (s/cat :input (s/merge :resource/base
                               (s/keys :opt-un [:resource/source])))
  :ret (s/merge :resource/base
                (s/keys :req-un [:resource/source])
                (s/keys :req-un [:resource/metadata])))

(s/fdef process/markdown
  :args (s/cat :input :resource/payload)
  :ret :resource/payload)

(s/fdef process/template
  :args (s/cat :input (s/keys :req-un [:resource/template]))
  :ret :resource/payload)

(s/fdef process/explode-redirects
  :args (s/cat :input (s/coll-of :resource/payload))
  :ret (s/coll-of :resource/payload))

(s/fdef process/verify
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/uri])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/output-location
  :args (s/cat :input (s/keys :req-un [:resource/uri]))
  :ret (s/merge :resource/base
                (s/keys :req-un [:resource/output])))

;;; High-level source->resource and source->templated functions

(s/fdef process/processed-resources
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/source])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/reverse-chronological-sort
  :args (s/cat :input (s/coll-of :resource/payload))
  :ret (s/coll-of :resource/payload))

(s/fdef process/standalone-resources
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/source])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/copy
  :args (s/cat :input (s/coll-of (s/keys :req-un [:resource/source])))
  :ret (s/coll-of :resource/payload))

(s/fdef process/template-directly
  :args (s/cat :layout :template/layout
               :uri :resource/uri
               :content :resource/content
               :extra (s/* (s/cat :opt keyword? :val any?)))
  :ret :resource/payload)

(s/fdef process/template-paginated
  :args (s/alt :no-uri (s/cat :layout :template/layout
                              :n-per-page integer?
                              :nested any?)
               :uri    (s/cat :layout :template/layout
                              :n-per-page integer?
                              :uri-seq (s/every :resource/uri)
                              :nested any?))
  :ret (s/coll-of :resource/payload))

(s/fdef views/base
  :args (s/cat :input :resource/full)
  :ret vector?)

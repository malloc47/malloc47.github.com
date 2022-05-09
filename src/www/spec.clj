(ns www.spec
  (:require [clojure.spec.alpha :as s])
  (:import (java.time LocalDate)))

(s/def :general/date
  (s/or :inst inst?
        :local-date (partial instance? LocalDate)))

(s/def :resource/date :general/date)
(s/def :resource/date-str string?)
(s/def :resource/permalink string?)
(s/def :resource/layout
  (s/or
   :fixed #{:page :post :home :redirect}
   :filename (s/and
              keyword?
              (comp (partial re-matches #"\w+\.\w+$")
                    name))))
(s/def :resource/format keyword?)
(s/def :resource/title string?)
(s/def :resource/draft? boolean?)
(s/def :resource/redirect? boolean?)
(s/def :resource/redirects (s/or :single string?
                                 :multiple (s/coll-of string?)))
(s/def :resource/filename string?)
(s/def :resource/uri string?)
(s/def :resource/modified inst?)
(s/def :resource/content string?)

(s/def :site/title string?)
(s/def :site/url string?)
(s/def :site/author string?)
(s/def :site/email string?)
(s/def :site/description string?)
(s/def :site/time inst?)

(s/def :resource/payload
  (s/keys :req-un [:resource/layout
                   :resource/title
                   :resource/uri]
          :opt-un [:resource/content
                   :resource/filename
                   :resource/format
                   :resource/draft?
                   :resource/redirects
                   :resource/modified
                   :resource/date]))

(s/def :resource/metadata
  (s/keys :req-un [:resource/title
                   :resource/layout]
          :opt-un [:resource/draft?
                   :resource/redirects
                   :resource/permalink
                   :resource/date-str]))

(s/def :resource/site
  (s/keys :req-un [:site/title
                   :site/url
                   :site/author
                   :site/email
                   :site/description
                   :site/time]))

(s/def :resource/raw (s/map-of :resource/uri :resource/content))

(s/def :resource/lifted
  (s/keys :req-un [:resource/content :resource/filename]))

(s/def :resource/split
  (s/keys :req-un [:resource/content :resource/filename :resource/metadata]))

(s/def :resource/full
  (s/merge :resource/payload (s/keys :req-un [:site/site :resource/site])))

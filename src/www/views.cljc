(ns www.views
  (:require
   [rum.core :as rum]
   [www.config :refer [config]]))

(defn meta-tags
  [{{:keys [title url author description]} :site}]
  (list
   [:meta {:charset "utf-8"}]
   [:meta {:name "description" :content description}]
   [:meta {:name "keywords" :content ""}]
   [:meta {:name "author" :content author}]
   ;; TODO: add page uri to url
   [:meta {:property "og:url" :content url}]
   [:meta {:property "og:title" :content title}]
   [:meta {:property "og:type" :content "article"}]
   [:meta {:name "viewport" :content "width=720"}]))

(def font-preload
  (list
   [:link
    {:rel "preload"
     :href "/font/Existence-Light-webfont.woff2"
     :as "font"
     :type "font/woff2"
     :crossorigin true}]
   [:link
    {:rel "preload"
     :href "/font/Gudea-Regular-webfont.woff2"
     :as "font"
     :type "font/woff2"
     :crossorigin true}]
   [:link
    {:rel "preload"
     :href "/font/Gudea-Italic-webfont.woff2"
     :as "font"
     :type "font/woff2"
     :crossorigin true}]
   [:link
    {:rel "preload"
     :href "/font/Gudea-Bold-webfont.woff2"
     :as "font"
     :type "font/woff2"
     :crossorigin true}]
   [:link
    {:rel "preload"
     :href "/font/Inconsolata-webfont.woff2"
     :as "font"
     :type "font/woff2"
     :crossorigin true}]))

(def css
  (list
   [:link {:rel "stylesheet" :href "/assets/style.css"}]
   [:link#day-css
    {:rel "stylesheet"
     :href "/assets/night.css"
     :disabled true}]
   [:link#night-css
    {:rel "stylesheet"
     :href "/assets/day.css"
     :disabled true}]))

(def js
  [:script {:src "/assets/main.js"}])

(rum/defc dark-mode-controls
  []
  [:div.icons-left
   [:canvas#miniclock.selectable {:width "50" :height "50"}]
   [:span#toggle.transparent-light.toggle.selectable]])

(def icons
  [:div.icons
   [:a.transparent.gmail {:href "mailto:malloc47@gmail.com"}]
   [:a.transparent.github {:href "https://www.github.com/malloc47"}]])

(def header
  [:header#main-header
   [:p "malloc(" [:span.header-number [:a {:href "/"} "47"]] ")"]
   [:p.header-name
    "—"
    [:a {:href "https://www.twitter.com/malloc47"} (-> config :site :author)]
    "—"]])

(def nav
  [:nav
   [:ul
    [:li [:a {:href "/" :title "Home"} "Home"]]
    [:li [:a {:href "/about" :title "About"} "About"]]
    [:li [:a {:href "/music" :title "Music"} "Music"]]
    [:li [:a {:href "/research" :title "Research"} "Research"]]
    [:li [:a {:href "/talks" :title "Talks"} "Talks"]]
    [:li [:a {:href (str "mailto:" (-> config :site :email)) :title "Email"}
          "Contact"]]]])

(def footer
  [:footer
   [:div.icons [:a.transparent.rss {:href "/rss.xml"}]]
   [:div.footer-padding
    "This site is licensed under "
    [:a
     {:rel "license"
      :href "https://creativecommons.org/licenses/by/3.0/"}
     "Creative Commons Attribution 3.0"]
    "."]])

(rum/defc page
  [payload]
  [:div.content
   [:div.content-wrap
    [:header "TITLE"]
    [:div.content-body
     {:dangerouslySetInnerHTML {:__html (:content payload)}}]]])

(rum/defc single-post
  ([payload]
   (single-post payload false))
  ([payload link-title?]
   [:div.content
    [:div.content-wrap
     [:p.date "yyyy" [:span.date-dark "00"] "MM" [:span.date-dark "00" "dd"]]
     [:p.social
      [:a
       {:href "https://www.facebook.com/sharer.php?u=&amp;t="
        :target "_blank"
        :rel "nofollow"
        :title "Share on Facebook"}
       "F"]
      [:a
       {:href "https://twitter.com/intent/tweet?url=&amp;text=&amp;via=malloc47&amp;related=malloc47"
        :target "_blank"
        :rel "nofollow"
        :title "Tweet"}
       "T"]]
     [:header (if link-title? [:a {:href (:uri payload)} "TITLE"] "TITLE")]
     [:div.content-body
      {:dangerouslySetInnerHTML {:__html (:content payload)}}]]]))

(rum/defc post
  [payload]
  (conj (single-post payload true)
        [:div.share
         [:a.twitter-share-button {:href "https://twitter.com/share" :data-via "malloc47"} "Tweet"]]))

(rum/defc base
  []
  (let [{{:keys [title]} :site} config
        payload {:layout :post
                 :content "<h1>HIIIIIIIIIIII</h1>"
                 :uri "https://www.whereever.com"}]
    [:html {:lang "en"}
     [:head [:title title] (meta-tags payload) font-preload css
      [:body (dark-mode-controls) icons
       [:div#container header nav
        [:div#contents {:role "main"}
         (cond
           (sequential? payload) ""
           (= (:layout payload) :page) (page payload)
           (= (:layout payload) :post) (single-post payload))]
        footer]]]]))

(ns www.views
  (:require
   [rum.core :as rum]
   [www.config :refer [config]]))

(def meta-tags
  (let [{{:keys [title url author description]} :site} config]
    (list
     [:meta {:charset "utf-8"}]
     [:meta {:name "description" :content description}]
     [:meta {:name "keywords" :content ""}]
     [:meta {:name "author" :content author}]
     ;; TODO: add page uri to url
     [:meta {:property "og:url" :content url}]
     [:meta {:property "og:title" :content title}]
     [:meta {:property "og:type" :content "article"}]
     [:meta {:name "viewport" :content "width=720"}])))

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

(def icons-left
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

(rum/defc base
  []
  (let [{{:keys [title]} :site} config
        content "wat"]
    [:html {:lang "en"}
     [:head [:title title] meta-tags font-preload css
      [:body icons-left icons
       [:div#container header nav
        [:div#contents {:role "main"} content]
        footer]]]]))

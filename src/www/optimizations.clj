(ns www.optimizations
  (:require
   [clojure.string :as str]
   [optimus.optimizations :as optimus]
   [optimus-autoprefixer.core :as prefixer]))

(defn all [assets options]
  (-> assets
      (optimus/minify-js-assets options)
      (optimus/minify-css-assets options)
      (optimus/inline-css-imports)
      (prefixer/prefix-css-assets options)
      (optimus/concatenate-bundles options)
      #_(optimus/add-cache-busted-expires-headers)
      (optimus/add-last-modified-headers)))

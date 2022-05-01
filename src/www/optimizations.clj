(ns www.optimizations
  (:require
   [clojure.string :as str]
   [optimus.optimizations :as optimus]))

(defn all [assets options]
  (-> assets
      #_(optimus/minify-js-assets options)
      (optimus/minify-css-assets options)
      (optimus/inline-css-imports)
      (optimus/concatenate-bundles options)
      #_(optimus/add-cache-busted-expires-headers)
      (optimus/add-last-modified-headers)))

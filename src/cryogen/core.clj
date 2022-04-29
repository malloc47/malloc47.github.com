(ns cryogen.core
  (:require [cryogen-core.compiler :refer [compile-assets-timed]]
            [cryogen-core.plugins :refer [load-plugins]]
            [cryogen.patch :refer [patch update-article-fn]]))

(defn -main []
  (patch)
  (load-plugins)
  (compile-assets-timed {:update-article-fn update-article-fn})
  (System/exit 0))

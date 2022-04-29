(ns cryogen.patch
  (:require [cryogen-core.compiler]))

(defn noop [& args])

(defn create-preview-patched [_ post] post)

(defn patch []
  (intern 'cryogen-core.compiler 'compile-tags-page noop)
  (intern 'cryogen-core.compiler 'compile-archives noop)
  (intern 'cryogen-core.compiler 'compile-authors noop)
  ;; I don't want previews on the home page, I want full articles
  (intern 'cryogen-core.compiler 'create-preview create-preview-patched))

(defn update-article-fn [{:keys [permalink] :as article} config]
  (cond-> article
    permalink (assoc :uri (str "/" permalink "/"))))

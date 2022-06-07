(ns www.parser.remark
  (:require
   [clojure.java.io :as io])
  (:import
   (org.graalvm.polyglot Context Source Engine)))

(def engine (Engine/create))

(def context
  (doto (-> (Context/newBuilder (into-array String ["js"]))
            (.engine engine)
            ;; (.option "js.timer-resolution" "1")
            ;; (.option "js.java-package-globals" "false")
            (.out System/out)
            (.err System/err)
            (.allowAllAccess true)
            (.allowNativeAccess true)
            (.build))
    (.eval (->> (io/file "js" "markdown" "lib.js")
                (Source/newBuilder "js")
                (.build)))))

(defn markdown
  [markdown-string]
  ;; Modifying the top-level bindings is a better, more lossless way
  ;; to transfer data into the context than trying to nest java
  ;; strings into javascript strings and deal with encoding, etc.
  (let [bindings (.getBindings context "js")]
    ;; TODO: This is just asking for a race condition
    (.putMember bindings "markdownString" markdown-string)
    (try
      (->> (str "www.parser.remark.markdown(markdownString)")
           (.eval context "js")
           (.asString))
      (finally (.removeMember bindings "markdownString")))))

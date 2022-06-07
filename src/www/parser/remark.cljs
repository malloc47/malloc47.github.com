(ns www.parser.remark
  (:require
   ["@mapbox/rehype-prism" :as rehypePrism]
   ["rehype-stringify$default" :as rehypeStringify]
   ["remark-parse$default" :as remarkParse]
   ["remark-rehype$default" :as remarkRehype]
   ["unified" :refer [unified]]))

(def ^:const processor
  (-> (unified)
      (.use remarkParse)
      (.use remarkRehype #js {:allowDangerousHtml true})
      (.use rehypePrism #js {:alias #js {:makefile #js ["make"]
                                         :lisp #js ["cl"]}})
      (.use rehypeStringify #js {:allowDangerousHtml true
                                 :closeSelfClosing true})))

(defn ^:export markdown
  [s]
  (str (.processSync processor s)))

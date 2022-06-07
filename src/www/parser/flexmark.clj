(ns www.parser.flexmark
  (:import
   (com.vladsch.flexmark.ext.attributes AttributesExtension)
   (com.vladsch.flexmark.ext.footnotes FootnoteExtension)
   (com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughExtension)
   (com.vladsch.flexmark.ext.superscript SuperscriptExtension)
   (com.vladsch.flexmark.ext.tables TablesExtension)
   (com.vladsch.flexmark.html HtmlRenderer)
   (com.vladsch.flexmark.util.data MutableDataSet)
   (java.util ArrayList)
   (com.vladsch.flexmark.parser Parser)))

(def extensions
  [(FootnoteExtension/create)
   (StrikethroughExtension/create)
   (SuperscriptExtension/create)
   (TablesExtension/create)
   (AttributesExtension/create)])

(def options
  (-> (MutableDataSet.)
      (.set Parser/EXTENSIONS (ArrayList. extensions))
      (.set HtmlRenderer/FENCED_CODE_LANGUAGE_CLASS_PREFIX "")
      (.set HtmlRenderer/GENERATE_HEADER_ID true)
      (.set HtmlRenderer/RENDER_HEADER_ID true)))

(def parser
  (.build (Parser/builder options)))

(def renderer
  (.build (HtmlRenderer/builder options)))

(defn markdown
  [markdown-string]
  (->> markdown-string
       (.parse parser)
       (.render renderer)))

(ns www.process
  (:require
   [www.parser :as parser]
   [clojure.string :as str]))

(defn lift
  "Convert single map of paths->contents into individual maps."
  [m]
  (map (fn [[k v]] {:content v :filename k}) m))

(defn metadata
  [{:keys [content] :as m}]
  (->> content
       parser/metadata
       (merge m)))

(defn trailing-slash
  [s]
  (cond-> s (not (str/ends-with? s "/")) (str "/")))

(defn normalize
  [{:keys [metadata filename] :as m}]
  (let [format (keyword (or (:format metadata)
                            (re-find #"\.\w+$" filename)
                            :unknown))
        uri    (trailing-slash
                (or (:permalink metadata)
                    (get (re-matches #"(.+)(\.\w+)$" filename) 1)
                    filename))]
    (->> {:format format
          :uri    uri}
         (merge m))))

(defn markdown
  [{:keys [format] :as m}]
  (cond-> m
    (= format :md)
    (update :content parser/markdown)))

(defn return
  [ms]
  (->> ms
       (map (fn [{:keys [uri content]}] [uri content]))
       (into {})))

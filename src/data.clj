(ns data
  (:require
   [clojure.data.json :as json]
   [org.httpkit.client :as http]
   #_[clj-http.client :as client]))


(defn get-news []
  (->> "https://hacker-news.firebaseio.com/v0/topstories.json"
       http/get
       deref
       :body
       json/read-json))

(defn get-news-item [id]
  (-> (str "https://hacker-news.firebaseio.com/v0/item/" id ".json")
      http/get))

(defn process-news-item [item]
  (-> item
      :body
      json/read-json
      (select-keys [:title :url :id])))

(defn get-news-details [ids]
  (let [calls (doall (map get-news-item ids))
        data  (doall (map deref calls))]
    (doall (map process-news-item data))))


(defn get-news-from-page [page page-size]
  (let [news (take page-size (drop (* page page-size) (get-news)))]
    (get-news-details news)))



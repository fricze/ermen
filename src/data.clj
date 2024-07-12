(ns data
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as client]))


(defn get-news []
  (->> "https://hacker-news.firebaseio.com/v0/topstories.json"
       client/get
       :body
       json/read-json))

(defn get-news-item [id]
  (->> (str "https://hacker-news.firebaseio.com/v0/item/" id ".json")
       client/get
       :body
       json/read-json))

(defn get-news-details [ids]
  (map get-news-item ids))

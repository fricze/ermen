(ns starter
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as client]
   [clojure.java.browse :as browse]
   [io.github.humbleui.ui :as ui]
   [io.github.humbleui.signal :as signal]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.font :as font]))

(defonce *news
  (signal/signal []))

(defonce *loading
  (signal/signal false))

(defonce *window
  (atom nil))

(defn redraw!
  "Requests a redraw on the next available frame."
  []
  (some-> @*window window/request-frame))



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

(defn get-news-details []
  (map get-news-item (get-news)))

(nth (get-news-details) 8)

(defn with-font []
  (let [{:keys [face-ui scale]} ui/*ctx*
        font (font/make-with-cap-height face-ui (* 12 scale))]
    [ui/center
     [ui/column {:gap 30}
      [ui/align {:x :left}
       [ui/row {:gap 10}
        [ui/button
         {:on-click (fn [_]
                      (future
                        (reset! *loading true)
                        (reset! *news (take 10 (get-news-details)))
                        (reset! *loading false)))}
         [ui/label {:font font} "Fetch"]]
        [ui/align
         {:y :center}
         [ui/label {:font font}
          (if (deref *loading) "Loading..." "Loaded.")]]]]

      [ui/column {:gap 20}
       (map (fn [{:keys [title]}]
              [ui/paragraph {:font font} title])
            (deref *news))]]]))


(ui/defcomp app []
  [with-font])


(redraw!)


(defn -main [& args]
  (ui/start-app!
   (reset! *window
           (ui/window
            {:title    "HumbleUI Modal Example"
             :bg-color 0xFFFFFFFF}
            #'app))))


#_(-main)

(ns starter
  (:require
   [clojure.data.json :as json]
   [clj-http.client :as client]
   [clojure.java.browse :as browse]
   [io.github.humbleui.ui :as ui]
   [io.github.humbleui.signal :as signal]
   [io.github.humbleui.paint :as paint]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.font :as font])
  (:import [java.awt.datatransfer DataFlavor StringSelection Transferable]))


(defn clipboard []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn clipboard-slurp []
  (try
    (.getTransferData (.getContents (clipboard) nil) (DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn clipboard-spit [text]
  (let [selection (StringSelection. text)]
    (.setContents (clipboard) selection selection)))

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


(defn tooltip [opts child]
  [ui/tooltip
   (update opts :tip
           (fn [text]
             [ui/rect {:paint (paint/fill 0xFFE9E9E9)}
              [ui/padding {:padding 10}
               [ui/label text]]]))
   child])


(defn link [url child]
  [ui/clickable
   {:on-click (fn [_] (browse/browse-url url))}
   child])

(defn text-color [color child]
  [ui/with-context
   {:fill-text (paint/fill color)}
   child])

(defn with-font []
  (let [{:keys [face-ui scale]} ui/*ctx*
        font                    (font/make-with-cap-height face-ui (* 12 scale))]
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
        (let [loading? (deref *loading)]
          [text-color
           (if loading? 0xFFFF82FF 0xFF0082FF)
           [ui/align
            {:y :center}
            [ui/label {:font font}
             (if loading? "Loading..." "Loaded.")]]])]]

      [ui/column {:gap 20}
       (map (fn [{:keys [title url]}]
              [ui/row
               #_[ui/clickable
                {:on-click (fn [_] (clipboard-spit url))}
                [ui/label "copy"]]
               [link
                url
                [tooltip
                 {:shackle :top-left
                  :anchor  :bottom-left
                  :tip     "You click me if you want"}
                 [ui/paragraph {:font font} title]]]])
            (deref *news))]]]))


(ui/defcomp app []
  [with-font])





(defn -main [& args]
  (ui/start-app!
    (reset! *window
            (ui/window
             {:title    "HumbleUI Modal Example"
              :bg-color 0xFFFFFFFF}
             #'app))))





(comment

  (redraw!)

  (-main)


  )

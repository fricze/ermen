(ns starter
  (:require [data :refer [get-news-details get-news]]
            [clojure.java.browse :as browse]
            [io.github.humbleui.ui :as ui]
            [io.github.humbleui.signal :as signal]
            [io.github.humbleui.paint :as paint]
            [io.github.humbleui.window :as window]
            [link :as link]
            [io.github.humbleui.font :as font]
            [clipboard]))




(defonce *news
  (signal/signal []))

(defonce *loading
  (signal/signal false))

(defonce *window
  (atom nil))

(defonce *page
  (signal/signal 0))


(defn redraw!
  "Requests a redraw on the next available frame."
  []
  (some-> @*window window/request-frame))


(def page-size 5)

(defn load-news [page]
  (reset! *loading true)
  (reset! *news (get-news-details (take page-size (drop (* page page-size) (get-news)))))
  (reset! *loading false))

(ui/defcomp header []
  (let [{:keys [face-ui scale]} ui/*ctx*
        font                    (font/make-with-cap-height face-ui (* 12 scale))]
    [ui/align {:x :left}
     [ui/row {:gap 10}
      [ui/button
       {:on-click (fn [_] (future (load-news 0)))}
       [ui/label {:font font} "Fetch"]]

      [ui/button
       {:on-click (fn [_]
                    (future
                      (reset! *news [])))}

       [ui/label {:font font} "Clear"]]

      (let [loading? (deref *loading)]
        [ui/align
         {:y :center}
         [ui/label {:paint (paint/fill (if loading? 0xFFFF82FF 0xFF0082FF))}
          (if loading? "Loading..." "")]])]]))

(ui/defcomp content []
  [ui/padding
   {:bottom 140}
   [ui/vscrollbar
    [ui/with-context
     {:font-size 18}
     [ui/column {:gap 20}
      (map (fn [{:keys [title url]}]
             [ui/align
              {:x :left}
              [link/link
               #(browse/browse-url url)
               title]])
           (take page-size (deref *news)))]]]])


(ui/defcomp pages []
  [ui/row
   (let [news    (get-news)
         page-no (deref *page)]
     (for [i (range (/ (count news) page-size))]
       [ui/padding {:right 4}
        [ui/with-cursor {:cursor :pointing-hand}
         [ui/button
          {:on-click (fn [_]
                       (future
                         (reset! *page i)
                         (load-news i)))}
          [ui/label {:paint (if (= i page-no)
                              (paint/fill 0xFF000000)
                              (paint/fill 0xFFFFFFFF))}
           (str (inc i))]]]]))])


(defn with-font []
  (let [{:keys [face-ui scale]} ui/*ctx*
        font                    (font/make-with-cap-height face-ui (* 12 scale))]
    [ui/padding
     {:padding 20}
     [ui/column {:gap 30}
      [header]
      [pages]
      [content]]]))


(ui/defcomp app []
  [with-font])



(defn -main [& args]
  (ui/start-app!
    (reset! *window
            (ui/window
             {:title    "Hacker News"
              :bg-color 0xFFFFFFFF}
             #'app))))


(comment

  (redraw!)

  (-main)


  )

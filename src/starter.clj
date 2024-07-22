(ns starter
  (:require [data :refer [get-news-from-page]]
            [clojure.java.browse :as browse]
            [io.github.humbleui.ui :as ui]
            [io.github.humbleui.signal :as signal]
            [io.github.humbleui.paint :as paint]
            [io.github.humbleui.window :as window]
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

(def page-size 20)

(defn load-news [page]
  (reset! *page page)
  (reset! *loading true)
  (future
    (let [news (get-news-from-page page page-size)]
      (reset! *news news)
      (reset! *loading false))))


(ui/defcomp app-state []
  (let [loading? (deref *loading)]
    [ui/align
     {:y :center}
     [ui/label {:paint (paint/fill (if loading? 0xFFFF82FF 0xFF0082FF))}
      (if loading? "Loading..." "")]]))

(ui/defcomp content []
  (let [news (deref *news)]
    [ui/with-context
     {:font-size 18}
     [ui/vscroll
      [ui/padding {:top 20 :bottom 20}
       [ui/column {:gap 20}
        (map
         (fn [{:keys [title url id]}]
           ^{:key id}
           [ui/align {:x :left}
            [ui/row {:gap 8}
             [ui/link
              #(browse/browse-url (str "https://news.ycombinator.com/item?id=" id))
              "[hn]"]
             [ui/link
              #(browse/browse-url (or url (str "https://news.ycombinator.com/item?id=" id)))
              title]]])
         news)]]]]))


(ui/defcomp pages []
  (let [page-no (deref *page)]
    [ui/column {:gap 20}
     [ui/row {:gap 8}
      (for [i (range 10)]
        [ui/with-cursor {:cursor :pointing-hand}
         [ui/button
          {:on-click (fn [_]
                       (load-news i)
                       true)}
          [ui/label
           {:paint (if (= page-no i)
                     (paint/fill 0xFFF2F2F2)
                     (paint/fill 0xFF000000))}
           (inc i)]]])]]))


(ui/defcomp app []
  [ui/padding
   {:padding 20
    :bottom 80}
   [ui/column {:gap 18}
    [app-state]
    [pages]
    [content]]])



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

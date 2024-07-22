(ns grid
  (:require [tech.v3.dataset :as ds]
            [clojure.string :as str]
            [io.github.humbleui.util :as util]
            [io.github.humbleui.paint :as paint]
            [io.github.humbleui.window :as window]
            [io.github.humbleui.signal :as signal]
            [text-field :refer [text-field]]
            [io.github.humbleui.ui :as ui])
  (:import [java.awt FileDialog Frame]
           [java.io FilenameFilter]))


(def *header
  (signal/signal [:head]))

(def *state
  (signal/signal
   {:sort-col   0
    :sort-dir   :asc
    :rows []}))

(defn load-csv [file]
  (let [ds      (ds/->dataset file)
        columns (ds/column-names ds)
        rows    (ds/rowvecs ds)]
    (reset! *header columns)
    (swap! *state (fn [state] (assoc state :rows (vec rows))))))



(defn -load-csv [file]
  (let [[head & tail]
        (->> (slurp file)
             (str/split-lines)
             (mapv #(str/split % #",")))]
    (reset! *header head)
    (swap! *state (fn [state] (assoc state :rows tail)))))

(comment
  (-load-csv "dev/currency.csv")
  (for [x (ds/->dataset "dev/currency.csv")]
    x)
  (ds/column-names (ds/->dataset "dev/currency.csv"))
  (first (ds/rowvecs (ds/->dataset "dev/currency.csv"))))

(defn csv-filename-filter []
  (proxy [FilenameFilter] []
    (accept [dir name]
      (.endsWith name ".csv"))))


(defn show-file-dialog []
  (let [file-dialog (FileDialog. (Frame.) "Choose a file" FileDialog/LOAD)]
    (.setFilenameFilter file-dialog (csv-filename-filter))
    (.setVisible file-dialog true)

    (let [file (.getFile file-dialog)
          dir  (.getDirectory file-dialog)]
      (if (and file dir)
        (let [path (str dir file)]
          (println "Selected file:" path)
          (load-csv path))
        (println "No file selected")))))



(defn on-click [i]
  (fn [e]
    (swap! *state
      (fn [{:keys [sort-col sort-dir rows] :as state}]
        (cond
          (not= i sort-col)
          {:sort-col   i
           :sort-dir   :asc
           :rows (->> rows (sort-by #(nth % i)))}

          (= :asc sort-dir)
          {:sort-col   i
           :sort-dir   :desc
           :rows (->> rows (sort-by #(nth % i)) reverse)}

          (= :desc sort-dir)
          {:sort-col   i
           :sort-dir   :asc
           :rows (->> rows (sort-by #(nth % i)))})))))

(def *search-columns (signal/signal {:text        ""
                                     :placeholder "Filter columns"
                                     :from 0
                                     :to 0}))

(def *search-rows (signal/signal {:text        ""
                                  :placeholder "Filter rows"
                                  :from 0
                                  :to 0}))

(ui/defcomp header []
  [ui/padding {:left  100
               :right 100
               :top   20}
   [ui/column {:gap 8}
    [ui/button
     {:on-click (fn [_]
                  (future (show-file-dialog)))}
     "Open file"]

    [ui/size {:width 300}
     [text-field "column"
      :padding-h 5
      :padding-v 10
      :*state *search-columns]]

    [ui/size {:width 300}
     [text-field "row"
      :padding-h 5
      :padding-v 10
      :*state *search-rows]]]])


(ui/defcomp ui []
  (let [{:keys [rows
                sort-col
                sort-dir]} @*state
        search-col         @*search-columns
        search-row         @*search-rows]
    [ui/column {:gap 8}
     [header]

     [ui/align {:y :center}
      [ui/vscroll
       [ui/align {:x :center}
        [ui/padding {:padding 20}
         [ui/grid {:cols (count @*header)
                   :rows (inc (count rows))}
          (concat
           (for [[th i] (util/zip @*header (range))]
             [ui/clickable
              {:on-click (on-click i)}
              (fn [{:keys [hovered]}]
                [ui/reserve-width
                 {:probes [[ui/label (str th " ⏶")]
                           [ui/label (str th " ⏷")]]}
                 [ui/align {:x :left}
                  [ui/row
                   #_[ui/checkbox {:*value (signal/signal true)} [ui/label ""]]
                   [ui/with-cursor {:cursor :pointing-hand}
                    [ui/label {:font-weight :bold
                               :paint (if hovered
                                        (paint/fill 0xFF000000)
                                        (paint/fill 0xFF808080))}
                     (str th
                          (case (when (= i sort-col)
                                  sort-dir)
                            :asc  " ⏶"
                            :desc " ⏷"
                            nil   ""))]]]]])])

           (let [rows (filter (fn [row] (re-find
                                         (re-pattern (str "(?i)" (:text search-col)))
                                         (first row)))
                              rows)
                 rows (filter (fn [row]
                                (some (fn [cell] (re-find
                                                  (re-pattern (str "(?i)" (:text search-row)))
                                                  cell))
                                      (rest row))) rows)]
             (for [row rows
                   s   row]
               [ui/padding {:padding 10}
                [ui/label s]])))]]]]]]))



(defonce *window
  (atom nil))

(defn -main [& args]
  (ui/start-app!
    (reset! *window
            (ui/window
             {:title    "Datasets"
              :bg-color 0xFFFFFFFF}
             #'ui))))



(defn redraw!
  "Requests a redraw on the next available frame."
  []
  (some-> @*window window/request-frame))

(comment

  (redraw!)

  (-main))

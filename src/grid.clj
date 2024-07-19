(ns grid
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [tech.v3.dataset :as ds]
   [clojure.string :as str]
   [io.github.humbleui.util :as util]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.signal :as signal]
   [io.github.humbleui.ui :as ui])
  (:import [java.awt FileDialog Frame]))


(def *header
  (signal/signal [:head]))

(def *state
  (signal/signal
   {:sort-col   0
    :sort-dir   :asc
    :currencies []}))

(defn load-csv [file]
  (println (ds/->dataset file))

  #_(let [[head & tail]
        (with-open [reader (io/reader file)]
          (doall
           (csv/read-csv reader)))]
    (reset! *header head)
    (swap! *state (fn [state] (assoc state :currencies tail)))))

(defn -load-csv [file]
  (let [[head & tail]
        (->> (slurp file)
             (str/split-lines)
             (mapv #(str/split % #",")))]
    (reset! *header head)
    (swap! *state (fn [state] (assoc state :currencies tail)))))

(-load-csv "dev/currency.csv")
(comment
  (for [x (ds/->dataset "dev/currency.csv")]
    x)
  (ds/column-names (ds/->dataset "dev/currency.csv"))
  (first (ds/rowvecs (ds/->dataset "dev/currency.csv"))))

(defn show-file-dialog []
  (let [file-dialog (FileDialog. (Frame.) "Choose a file" FileDialog/LOAD)]
    (.setVisible file-dialog true)
    (let [file (.getFile file-dialog)
          dir (.getDirectory file-dialog)]
      (if (and file dir)
        (let [path (str dir file)]
          (println "Selected file:" path)
          (load-csv path))
        (println "No file selected")))))


(defn on-click [i]
  (fn [e]
    (swap! *state
      (fn [{:keys [sort-col sort-dir currencies] :as state}]
        (cond
          (not= i sort-col)
          {:sort-col   i
           :sort-dir   :asc
           :currencies (->> currencies (sort-by #(nth % i)))}

          (= :asc sort-dir)
          {:sort-col   i
           :sort-dir   :desc
           :currencies (->> currencies (sort-by #(nth % i)) reverse)}

          (= :desc sort-dir)
          {:sort-col   i
           :sort-dir   :asc
           :currencies (->> currencies (sort-by #(nth % i)))})))))

(ui/defcomp ui []
  (let [{:keys [currencies
                sort-col
                sort-dir]} @*state]
    [ui/column
     [ui/button
      {:on-click (fn [_]
                   (future (show-file-dialog)))}
      "Open file"]
     [ui/align {:y :center}
      [ui/vscroll
       [ui/align {:x :center}
        [ui/padding {:padding 20}
         [ui/grid {:cols (count @*header)
                   :rows (inc (count currencies))}
          (concat
           (for [[th i] (util/zip @*header (range))]
             [ui/clickable
              {:on-click (on-click i)}
              [ui/padding {:padding 10}
               [ui/reserve-width
                {:probes [[ui/label (str th " ⏶")]
                          [ui/label (str th " ⏷")]]}
                [ui/align {:x :left}
                 [ui/label {:font-weight :bold}
                  (str th
                       (case (when (= i sort-col)
                               sort-dir)
                         :asc  " ⏶"
                         :desc " ⏷"
                         nil   ""))]]]]])
           (for [row (take 10 currencies)
                 s   row]
             [ui/padding {:padding 10}
              [ui/label s]]))]]]]]]))


(defonce *window
  (atom nil))

(defn -main [& args]
  (ui/start-app!
    (reset! *window
            (ui/window
             {:title    "Hacker News"
              :bg-color 0xFFFFFFFF}
             #'ui))))



(defn redraw!
  "Requests a redraw on the next available frame."
  []
  (some-> @*window window/request-frame))

(comment

  (redraw!)

  (-main))

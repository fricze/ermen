(ns grid-comp
  (:require [tech.v3.dataset :as ds]
            [clojure.string :as str]
            [io.github.humbleui.util :as util]
            [io.github.humbleui.paint :as paint]
            [io.github.humbleui.window :as window]
            [scroll :as scroll]
            [io.github.humbleui.signal :as signal]
            [text-field :refer [text-field]]
            [io.github.humbleui.ui :as ui]))

(defn- grid-maybe-measure [this cs ctx]
  (let [widths  (float-array (:cols this) 0)
        heights (float-array (:rows this) 0)
        max-col (dec (:cols this))]
    (util/loopr [row 0
                 col 0]
                [child (:children this)]
                (let [row' (if (< col max-col) row (inc row))
                      col' (if (< col max-col) (inc col) 0)]
                  (if child
                    (let [size (ui/measure child ctx cs)]
                      (aset-float widths  col (max (aget widths  col) (:width size)))
                      (aset-float heights row (max (aget heights row) (:height size)))
                      (recur row' col'))
                    (recur row' col'))))
    (util/set!! this
                :widths widths
                :heights heights)))

(util/deftype+ Grid [^:mut cols
                     ^:mut rows
                     ^:mut widths
                     ^:mut heights]
  :extends ui/AContainerNode

  (-measure-impl [this ctx cs]
                 (grid-maybe-measure this cs ctx)
                 (util/ipoint
                  (areduce ^floats widths  i res (float 0) (+ res (aget ^floats widths i)))
                  (areduce ^floats heights i res (float 0) (+ res (aget ^floats heights i)))))

  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
              (let [cs (util/ipoint (:width bounds) (:height bounds))]
                (grid-maybe-measure this cs ctx)
                (loop [x   (:x bounds)
                       y   (:y bounds)
                       row 0
                       col 0]
                  (let [height (aget ^floats heights row)
                        width  (aget ^floats widths col)]
                    (when-some [child (nth children (+ col (* row cols)) nil)]
                      (let [child-bounds (util/irect-xywh x y width height)]
                        (when (util/irect-intersect child-bounds viewport)
                          (ui/draw child ctx child-bounds viewport canvas))))
                    (cond
                      ;; no more cols and rows
                      (and (>= col (dec cols)) (>= row (dec rows)))
                      :done

                      ;; end of viewport at the bottom
                      (and (>= col (dec cols)) (> (+ y height) (:bottom viewport)))
                      :done ;; skip rest of cols

                      ;; end of row
                      (>= col (dec cols))
                      (recur (:x bounds) (+ y height) (inc row) 0)

                      ;; end of viewport on the right
                      (and (>= col (dec cols)) (>= (+ x width) (+ 20 (:right viewport)))) ;; skip rest of current row
                      (recur (:x bounds) (+ y height) (inc row) 0)

                      ;; next col in the same row
                      :else
                      (recur (+ x width) y row (inc col)))))))

  (-reconcile-opts [_ ctx new-element]
                   (let [opts  (ui/parse-opts new-element)
                         cols' (util/checked-get opts :cols pos-int?)
                         rows' (or
                                (util/checked-get-optional opts :rows pos-int?)
                                (-> (count children) dec (quot cols') inc))]
                     (when (or
                            (not= cols cols')
                            (not= rows rows'))
                       (set! cols cols')
                       (set! rows rows')
                       (set! widths nil)
                       (set! heights nil)))))

(defn grid
  ([opts]
   (grid opts []))
  ([opts & children]
   (map->Grid {})))



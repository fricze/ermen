(ns scroll
  (:require [clojure.math :as math]
            [io.github.humbleui.ui :as ui]
            [io.github.humbleui.util :as util]
            [io.github.humbleui.canvas :as canvas]
            [io.github.humbleui.protocols :as protocols]
            [io.github.humbleui.signal :as signal]
            [io.github.humbleui.window :as window]))


(util/deftype+ Scrollable [^:mut clip?
                           ^:mut offset-y-px
                           ^:mut offset-y
                           ^:mut offset-x-px
                           ^:mut offset-x
                           ^:mut child-size]
  :extends ui/AWrapperNode

  (-measure-impl [_ ctx cs]
                 (let [child-cs (assoc cs
                                       :height Integer/MAX_VALUE
                                       :width Integer/MAX_VALUE)]

                   (set! child-size (protocols/-measure child ctx child-cs))

                   (util/ipoint
                    (min
                     (:width child-size)
                     (:width cs))
                    (min
                     (:height child-size)
                     (:height cs)))))

  (-draw-impl [_ ctx bounds viewport canvas]
              (set! child-size
                    (protocols/-measure
                     child ctx
                     (util/ipoint (:width bounds) Integer/MAX_VALUE)))

              (set! offset-y-px
                    (util/clamp (ui/scaled (or @offset-y 0)) 0 (- (:height child-size) (:height bounds))))

              (set! offset-x-px (util/clamp
                                 (ui/scaled (or @offset-x 0))
                                 0
                                 (Math/abs (- (:width child-size) (:width bounds)))))

              (canvas/with-canvas canvas
                (when clip?
                  (canvas/clip-rect canvas bounds))

                (let [child-bounds (-> bounds
                                       (update :x - offset-x-px)
                                       (update :x math/round)
                                       (assoc :width (:width child-size))

                                       (update :y - offset-y-px)
                                       (update :y math/round)
                                       (assoc :height (:height child-size)))]

                  (ui/draw child ctx child-bounds (util/irect-intersect viewport bounds) canvas))))

  (-event-impl [_ ctx event]
               (case (:event event)
                 :mouse-scroll
                 (when (util/rect-contains? bounds (util/ipoint (:x event) (:y event)))
                   (or
                    (ui/event child ctx event)

                    (let [offset-x-px' (-> offset-x-px
                                           (- (:delta-x event))
                                           (util/clamp 0 (Math/abs (- (:width child-size) (:width bounds)))))
                          offset-y-px' (-> offset-y-px
                                           (- (:delta-y event))
                                           (util/clamp 0 (- (:height child-size) (:height bounds))))]

                      (when (not= offset-x-px offset-x-px')
                        (set! offset-x-px offset-x-px')
                        (reset! offset-x (ui/descaled (math/round offset-x-px')))
                        (window/request-frame (:window ctx)))

                      (when (not= offset-y-px offset-y-px')
                        (set! offset-y-px offset-y-px')
                        (reset! offset-y (ui/descaled (math/round offset-y-px')))
                        (window/request-frame (:window ctx))))))

                 :mouse-button
                 (when (util/rect-contains? bounds (util/ipoint (:x event) (:y event)))
                   (ui/event child ctx event))

                 #_:else
                 (ui/event child ctx event)))

  (-reconcile-opts [_this _ctx new-element]
                   (let [opts (ui/parse-opts element)]
                     (set! clip? (:clip? opts true))

                     (when-some [offset-x' (:offset-x opts)]
                       (set! offset-x offset-x'))

                     (when-some [offset-y' (:offset-y opts)]
                       (set! offset-y offset-y')))))


(defn hscrollable
  ([child]
   (hscrollable {} child))
  ([opts child]
   (map->HScrollable
    {:offset-y-px 0
     :offset-y    (or
                   (:offset-y opts)
                   (signal/signal 0))})))


(defn vscroll
  ([child]
   (vscroll {} child))
  ([opts child]
   (map->Scrollable
    {:offset-y-px 0
     :offset-y    (or
                   (:offset-y opts)
                   (signal/signal 0))
     :offset-x-px 0
     :offset-x    (or
                   (:offset-x opts)
                   (signal/signal 0))})))

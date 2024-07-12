(ns link
  (:require [io.github.humbleui.ui :as ui]
            [io.github.humbleui.signal :as signal]
            [io.github.humbleui.paint :as paint]
            [io.github.humbleui.font :as font]
            [clipboard]))


(ui/defcomp link [{:keys [color
                          color-visited
                          color-hovered
                          color-pressed
                          on-click
                          visited]
                   :or   {color         0xFF0071FF
                          color-visited 0xFFB30EB3
                          color-hovered 0xFFEE0000
                          on-click      #()}}
                  child]
  (let [color-pressed (or
                       color-pressed
                       color-hovered)
        visited       (or visited (signal/signal false))
        font          (ui/get-font)
        metrics       (font/metrics font)
        position      (ui/descaled (:underline-position metrics))
        thickness     (ui/descaled (:underline-thickness metrics))]
    (ui/with-resources [paint         (paint/fill color)
                        paint-visited (paint/fill color-visited)
                        paint-hovered (paint/fill color-hovered)
                        paint-pressed (paint/fill color-pressed)]
      (fn [on-click child]
        [ui/center
         [ui/with-cursor {:cursor :pointing-hand}
          [ui/clickable {:on-click (fn [_event]
                                     (reset! visited true)
                                     (on-click))}
           (fn [state]
             (let [paint (cond
                           (:pressed state) paint-pressed
                           (:hovered state) paint-hovered
                           @visited         paint-visited
                           :else            paint)]
               [ui/column
                [ui/label {:paint paint} child]
                [ui/size {:height position}]
                [ui/rect {:paint paint}
                 [ui/size {:height thickness}]]]))]]]))))

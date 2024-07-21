(ns text-field
  (:require [io.github.humbleui.signal :as signal]
            [io.github.humbleui.ui :as ui]))


(defn text-field [text & {:keys [from to placeholder cursor-blink-interval cursor-width padding-h padding-v padding-top padding-bottom border-radius *state]
                          :or   {cursor-blink-interval 500,
                                 cursor-width          1,
                                 padding-h             0,
                                 padding-v             3,
                                 border-radius         4
                                 *state                (signal/signal
                                                        {:text        text
                                                         :placeholder "from within"
                                                         })}
                          :as   opts}]
  (println @*state)
  [ui/with-context
   {:hui.text-field/cursor-blink-interval cursor-blink-interval
    :hui.text-field/cursor-width          cursor-width
    :hui.text-field/border-radius         border-radius
    :hui.text-field/padding-top           (float (or padding-top padding-v))
    :hui.text-field/padding-bottom        (float (or padding-bottom padding-v))
    :hui.text-field/padding-left          (float padding-h)
    :hui.text-field/padding-right         (float padding-h)}
   [ui/text-field
    (assoc opts :*state *state)]])

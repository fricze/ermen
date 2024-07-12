(ns clipboard 
  (:import [java.awt.datatransfer DataFlavor StringSelection]))

(defn clipboard []
  (.getSystemClipboard (java.awt.Toolkit/getDefaultToolkit)))

(defn clipboard-slurp []
  (try
    (.getTransferData (.getContents (clipboard) nil) (DataFlavor/stringFlavor))
    (catch java.lang.NullPointerException e nil)))

(defn clipboard-spit [text]
  (let [selection (StringSelection. text)]
    (.setContents (clipboard) selection selection)))

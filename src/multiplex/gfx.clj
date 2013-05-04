(ns multiplex.gfx
  (:require [multiplex.config :as config]
            [multiplex.util :as util]))

(defn read-image
  "returns a BufferedImage from a filename"
  [filename]
    (with-open [r (java.io.FileInputStream. filename)]
        (javax.imageio.ImageIO/read r)))

(defn image-size
  "returns width and height of a local image as a vector"
  [image]
  (try
    [(.getWidth image) (.getHeight image)]
  (catch Exception e
    (do
      (println (str "ERR: Reading size failed: " (str image)))
      [0 0]))))

(defn calc-resized
  [image]
  (let [sizes (image-size image)
        cfg config/img-max-size
        ratio-w (/ (first sizes) (first cfg))
        ratio-h (/ (second sizes) (second cfg))]
    (println (str sizes " " cfg "|" ratio-w))
    (if
      (>= ratio-w 1)
      [(/ (first sizes) ratio-w) (/ (second sizes) ratio-w)]
      sizes)))

(defn resize
  [image filename width-new height-new]
  (let [new-img (java.awt.image.BufferedImage. width-new height-new java.awt.image.BufferedImage/TYPE_INT_RGB)
        g (.createGraphics new-img)
        resized (.drawImage g image 0 0 nil)
        ext  (util/file-extension filename)
        new-name (str filename ".thumb." ext)]
    (println new-img)
    (println "\n")
    (println g)
    (println "\n")
    (println resized)
    (println "\n")
    (println new-name)
    (javax.imageio.ImageIO/write new-img ext (java.io.File. new-name))))
(ns multiplex.gfx
  (:require [multiplex.config :as config]
            [multiplex.util :as util]))

(defn read-image
  "returns a BufferedImage from a filename"
  [^String filename]
    (with-open [r (java.io.FileInputStream. filename)]
        (javax.imageio.ImageIO/read r)))

(defn image-size
  "returns width and height of a local BufferedImage as a vector"
  [^java.awt.image.BufferedImage image]
  (try
    [(.getWidth image) (.getHeight image)]
  (catch Exception e
    (do
      (println (str "ERR: Reading size failed: " (str image)))
      [0 0]))))

(defn image-mime
  "returns the mime type of an image via the filename"
  [filename]
  (let [uri (java.net.URI. (str "file://" filename))
        src (java.nio.file.Paths/get uri)]
    (java.nio.file.Files/probeContentType src)))

(defn num-frames
  [^String gif-path]
  (let [file (java.io.File. gif-path)
        irs (com.sun.imageio.plugins.gif.GIFImageReaderSpi.)
        ir (com.sun.imageio.plugins.gif.GIFImageReader. irs)
        foo (javax.imageio.ImageIO/createImageInputStream file)
        ir2 (doto ir (.setInput foo))]
     (.getNumImages ir2 true)))

(defn needs-resize?
  [orig-sizes new-sizes filename]
  (let [ext (util/file-extension filename)]
    (if (= "gif" ext)
      (if (= 1 (num-frames filename)) true false)
      (not= orig-sizes new-sizes))))

(defn calc-resized
  [image]
  (let [sizes (image-size image)
        cfg config/img-max-size
        ratio-w (/ (first sizes) (first cfg))
        ratio-h (/ (second sizes) (second cfg))]
    (if
      (>= ratio-w 1)
      [(int (/ (first sizes) ratio-w)) (int (/ (second sizes) ratio-w))]
      sizes)))

(defn resize
  "resize a BufferedImage and save as <filename.EXT.thumb.EXT>"
  [image filename width-new height-new]
  (let [new-img (java.awt.image.BufferedImage. width-new height-new java.awt.image.BufferedImage/TYPE_INT_RGB)
        g (.createGraphics new-img)
        resized (.drawImage g image 0 0 width-new height-new nil)
        ext ^String (util/file-extension filename)
        new-name (str filename ".thumb." ext)]
    (javax.imageio.ImageIO/write new-img ext (java.io.File. new-name))
    filename))

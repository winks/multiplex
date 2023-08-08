(ns multiplex.gfx-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer :all]
    [multiplex.config :as config]
    [multiplex.gfx :refer :all]))

(deftest test-read-image-1
 (is (= java.awt.image.BufferedImage (type (read-image "./resources/test/test_pic.png")))))

(deftest test-image-size-1
 (is (= [0 0] (image-size "")))
 (is (= [166 49] (image-size (read-image "./resources/test/test_pic1.gif"))))
 (is (= [49 166] (image-size (read-image "./resources/test/test_pic2.gif")))))

(deftest test-image-mime-1
 (is (= "image/png"     (image-mime (.getAbsolutePath (io/file "./resources/test/test_pic.png")))))
 (is (= "image/jpeg"    (image-mime (.getAbsolutePath (io/file "./resources/test/test_pic.jpg")))))
 (is (= "image/gif"     (image-mime (.getAbsolutePath (io/file "./resources/test/test_pic.gif")))))
 (is (= "image/svg+xml" (image-mime (.getAbsolutePath (io/file "./resources/test/test_pic.svg"))))))

(deftest test-num-frames-1
 (is (= 1 (num-frames "./resources/test/test_pic.gif")))
 (is (= 4 (num-frames "./resources/test/test_ani.gif"))))
(ns simplex-clj.util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clojure.java.io :as cjio]
            [digest :as digest]
            [simplex-clj.models.db :as db]
            [simplex-clj.config :as config]))


(defn format-time
  "formats the time using SimpleDateFormat, the default format is
   \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
  ([time] (format-time time "dd MMM, yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
    (md/md-to-html-string (io/slurp-resource filename)))

(defn user-from-authkey [authkey]
  (get config/users (keyword authkey)))

(defn valid-authkey? [authkey]
  (not (nil? (user-from-authkey authkey))))

(defn download-file [url filename]
  (cjio/copy (cjio/input-stream url) (cjio/output-stream filename)))

(defn hash-name [arg]
  (digest/sha1 arg))

(defn valid-post-type? [arg]
  (let [allowed (db/itemtypes)]
    (some #{arg} allowed)))

(defn file-extension [name]
  (let [parts (reverse (clojure.string/split name #"\."))
        ext (clojure.string/lower-case (first parts))]
    (if (.equals "jpeg" ext)
      "jpg"
      ext)))

(defn host-name [url]
  (let [x (clojure.string/replace url #"http(s)?://(www\.)?" "")]
    (first (clojure.string/split x #"/"))))

(defn guess-type [url txt]
  (if (empty? url)
    "text"
    (let [ext (file-extension url)
          host (host-name url)]
      (if (some #{host} config/sites-video)
        "video"
        (if (some #{ext} config/img-types)
          "image"
          "link")))))

(defn image-size [filename]
  (try
    (with-open [r (java.io.FileInputStream. filename)]
      (let [image (javax.imageio.ImageIO/read r)]
        [(.getWidth image) (.getHeight image)]))
    (catch Exception e
      (do
        (println (.printStackTrace e))
        [0 0]))))

(defn video-info [s]
  (let [host (host-name s)]
    (if
      (some #{host} config/sites-youtube)
      (let [matcher (re-matcher #"[\?&]v=([a-zA-Z0-9_-]+)" s)]
        {:site "youtube" :code (second (re-find matcher))})
      (if
        (some #{host} config/sites-vimeo)
        (let [matcher (re-matcher #"/([0-9]+)" s)]
          {:site "vimeo" :code (second (re-find matcher))})
        {:site "err" :code ""}))))


(defn int-or-default [s default]
  (if
    (empty? s)
    default
    (try
      (let [n (Integer/parseInt s)]
        (if (pos? n) n default))
      (catch Exception e
        default))))

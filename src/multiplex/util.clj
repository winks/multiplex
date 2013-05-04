(ns multiplex.util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clojure.java.io :as cjio]
            [digest :as digest]
            [multiplex.models.db :as db]
            [multiplex.config :as config]))


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

(defn download-file
  "copies an image from an URL to a local file"
  [url filename]
  (with-open [input (cjio/input-stream url)
              output (cjio/output-stream filename)]
    (cjio/copy input output)))

(defn valid-post-type?
  "determines if the given type of post is allowed"
  [arg]
  (let [allowed (config/itemtypes)]
    (some #{arg} allowed)))

(defn file-extension
  "gets the lower-cased file extension from a string"
  [name]
  (let [parts (reverse (clojure.string/split name #"\."))
        ext (clojure.string/lower-case (first parts))]
    (if (.equals "jpeg" ext)
      "jpg"
      ext)))

(defn host-name
  "gets the host name part from an url"
  [url]
  (let [x (clojure.string/replace url #"http(s)?://(www\.)?" "")]
    (first (clojure.string/split x #"/"))))

(defn guess-type
  "if the post type is not given, try guessing from contents"
  [url txt]
  (if (empty? url)
    "text"
    (let [ext (file-extension url)
          host (host-name url)]
      (if (some #{host} config/sites-video)
        "video"
        (if (some #{host} config/sites-audio)
          "audio"
          (if (some #{ext} config/img-types)
            "image"
            "link"))))))

(defn video-info
  "extract the unique part of a video url, e.g. from youtube.com/watch?v=FOO"
  [s]
  (let [host (host-name s)]
    (if
      (some #{host} config/sites-youtube)
      (let [matcher (re-matcher #"[\?&]v=([a-zA-Z0-9_-]+)" s)]
        {:site "youtube" :code (second (re-find matcher))})
      (if
        (some #{host} config/sites-vimeo)
        (let [matcher (re-matcher #"/([0-9]+)" s)]
          {:site "vimeo" :code (second (re-find matcher))})
        (if
          (some #{host} config/sites-soundcloud)
          {:site "soundcloud" :code ""}
          {:site "err" :code ""})))))


(defn int-or-default
  "try to coerce to integer or return a safe default"
  [s default]
  (if
    (empty? s)
    default
    (try
      (let [n (Integer/parseInt s)]
        (if (pos? n) n default))
      (catch Exception e
        default))))

(defn hash-filename [arg]
  (digest/sha1 arg))

(defn hash-api-token [arg]
  (digest/md5 arg))

(defn hash-password [arg]
  (digest/sha-512 arg))

(defn md-hash
  [algorithm arg]
  (let [md (java.security.MessageDigest/getInstance algorithm)
        digest (.digest md (.getBytes arg))]
    (.toString (BigInteger. 1 digest) 16)))

(defn md5
  [arg]
  (md-hash "MD5" arg))

(defn sha-512
  [arg]
  (md-hash "SHA-512" arg))

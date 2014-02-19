(ns multiplex.util
  (:require [noir.io :as io]
            [clojure.data.json :as json]
            [markdown.core :as md]
            [clojure.java.io :as cjio]
            [digest :as digest]
            [clj-time.format :as tformat]
            [multiplex.config :as config]))

(defn read-time
  [time]
  (tformat/parse (tformat/formatter "yyyy-MM-dd HH:mm:ss.S") time))

(defn put-time
  [time]
  (tformat/unparse (tformat/formatter "yyyy-MM-dd HH:mm") time))

(defn md->html
  "creates HTML from string"
  [s]
    (md/md-to-html-string s))

(defn mdfile->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
    (md->html (io/slurp-resource filename)))

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

(defn string-or-default
  ([s]
    (string-or-default s ""))
  ([s default]
    (if
      (empty? s)
      default
      (clojure.string/trim s))))

(defn int-or-default
  "try to coerce to integer or return a safe default"
  [s default]
  (try
    (let [n (if (instance? java.lang.Integer s)
                (long s)
                (if (instance? java.lang.Long s)
                    s
                    (if (instance? java.lang.String s)
                        (Long/valueOf s)
                        (if (instance? java.lang.Double s)
                            (long s)
                            default))))]
      (if (pos? n) n default))
    (catch Exception e
      default)))

(defn get-avatar [n]
  (let [prefix (str (:page-scheme config/multiplex) "://" (:static-url config/multiplex))]
    (str prefix (nth config/user-icons (int-or-default n 0)))))

(defn add-fields [coll]
  (let [info (video-info (:url coll))
        meta-foo (if (= "" (:meta coll)) "{}" (:meta coll))
        updated (put-time (read-time (str (:updated coll))))
        prefix (str (:page-scheme config/multiplex) "://" (:static-url config/multiplex))
        url (if (= config/rel-path (subs (:url coll) 0 (count config/rel-path)))
                (str prefix (:url coll))
                (:url coll))]
    (assoc coll :code (:code info)
                :site (:site info)
                :avatar (get-avatar (:author coll))
                :url url
                :static-url prefix
                :updated (or updated (:updated coll))
                :meta (json/read-str meta-foo :key-fn keyword))))

(defn hash-filename [arg]
  (digest/sha1 arg))

(defn hash-apikey [arg]
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

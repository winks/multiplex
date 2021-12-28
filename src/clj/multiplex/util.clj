(ns multiplex.util
  (:require [clojure.data.json :as json]
            [clojure.java.io :as cjio]
            [clojure.string :as str]
            [clj-time.format :as tformat]
            [multiplex.config :as config]))

;[markdown.core :as md]
;[digest :as digest]

;(defn read-time
;  [time]
;  (tformat/parse (tformat/formatter "yyyy-MM-dd HH:mm:ss.S") time))
;(defn put-time
;  [time]
;  (tformat/unparse (tformat/formatter "yyyy-MM-dd HH:mm") time))

(defn file-extension
  "gets the lower-cased file extension from a string"
  [name]
  (if (empty? name)
    ""
    (let [parts (reverse (str/split name #"\."))
          ext (re-find #"[a-z]+" (str/lower-case (first parts)))]
      (if (= "jpeg" ext)
        "jpg"
        ext))))

(defn make-url [scheme host]
  (str scheme "://" host))

(defn host-name
  "gets the host name part from an url"
  [url]
  (let [x (str/replace url #"http(s)?://(www\.)?" "")]
    (first (str/split x #"/"))))

(defn read-remote
  "read from remote url"
  [url default]
  (try
    (slurp url)
    (catch Exception ex default)))

(defn video-info
  "extract the unique part of a video url, e.g. from youtube.com/watch?v=FOO"
  [s]
  (let [host (host-name s)]
    (if (some #{host} config/sites-youtube)
      (let [matcher (re-matcher #"[\?&]v=([a-zA-Z0-9_-]+)" s)
            code (second (re-find matcher))]
        {:site "youtube" :code code :thumb-id code})
      (if (some #{host} config/sites-vimeo)
        (let [matcher (re-matcher #"/([0-9]+)" s)
              code (second (re-find matcher))
              json-data (read-remote (str "https://vimeo.com/api/oembed.json?url=https%3A//vimeo.com/" code) "{\"thumbnail_url\":\"\"}")
              asd (json/read-str json-data :key-fn keyword)
              matcher2 (re-matcher #"/video/([0-9]+)" (:thumbnail_url asd))
              thumb-id (second (re-find matcher2))]
          {:site "vimeo" :code code :thumb-id thumb-id :thumb-width (:thumbnail_width asd) :duration (:duration asd)})
        (if (some #{host} config/sites-soundcloud)
          (let [html (read-remote s "")
                matcher (re-matcher #"content=\"soundcloud://sounds:([0-9]+)\"" html)
                matcher2 (re-matcher #"og:image\" content=\"([^\"]+)\"" html)
                img (or (second (re-find matcher2)) "")
                ext (file-extension img)
                parts (str/split img #"/")]
            (if-let [code (second (re-find matcher))]
              {:site "soundcloud" :code code :thumb-id (str/replace (last parts) (str "." ext) "") :thumb-path (str/replace img (last parts) "") :thumb-ext ext}
              {:site "soundcloud" :code nil}))
          (if (and (some #{host} config/sites-imgur-gifv) (.endsWith s ".gifv"))
            (let [matcher (re-matcher #"https?://[^/]+/(.+)\.gifv$" s)]
              {:site "imgur-gifv" :code (second (re-find matcher))})
              {:site "err" :code ""}))))))

(defn thumbnail-url
  "get the thumbnail url for a video site"
  [m]
  (if (= "youtube" (:site m))
    (str "https://i.ytimg.com/vi/" (:code m) "/hqdefault.jpg")
    (if (= "vimeo" (:site m))
      (str "https://i.vimeocdn.com/video/" (:thumb-id m) ".jpg?mw=480"); (:thumb-width m))
      (if (= "soundcloud" (:site m))
        (str (:thumb-path m) (:thumb-id m) "." (:thumb-ext m))
        ""))))

(defn add-fields [coll author]
  (let [info (video-info (:url coll))
        meta-foo (if (= "" (str/trim (:meta coll))) "{}" (:meta coll))
        meta (json/read-str meta-foo :key-fn keyword)
        ;updated (:updated coll);(put-time (read-time (str (:updated coll))))
        prefix (make-url (:static-scheme (config/env :multiplex)) (:static-url (config/env :multiplex)))
        author (assoc {} :author (:uid author)
                         :username (:username author)
                         :hostname (:hostname author)
                         :url (make-url (:page-scheme (config/env :multiplex)) (:hostname author))
                         :avatar (:avatar author))
        url (if (< (count (:url coll)) (count (config/env :rel-path)))
                ""
                (if (= (config/env :rel-path) (subs (:url coll) 0 (count (config/env :rel-path))))
                    (str prefix (:url coll))
                    (:url coll)))]
    (assoc coll :code (:code info)
                :site (:site info)
                :url url
                ;:static-url prefix
                :thumb-path (str prefix (config/env :rel-path))
                ;:updated (or updated (:updated coll))
                :thumbnail (:thumbnail meta)
                :author author
                :meta meta)))

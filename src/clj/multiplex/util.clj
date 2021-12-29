(ns multiplex.util
  (:require [clojure.data.json :as json]
            [clojure.java.io :as cjio]
            [clojure.string :as str]
            [multiplex.config :as config]))

;[clj-time.format :as tformat]
;[markdown.core :as md]
;[digest :as digest]

(defn is-custom-host [hostname]
  (if (= (:page-url (config/env :multiplex)) hostname)
    false
    hostname))

;(defn read-time
;  [time]
;  (tformat/parse (tformat/formatter "yyyy-MM-dd HH:mm:ss.S") time))
;(defn put-time
;  [time]
;  (tformat/unparse (tformat/formatter "yyyy-MM-dd HH:mm") time))

(defn valid-post-type?
  "determines if the given type of post is allowed"
  [arg]
  (if (empty? arg)
    false
    (some #(= arg %) config/itemtypes)))

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

(defn make-url [scheme host & [params]]
  (let [port (:server-port (or params {:server-port 0}))
        port (if (> port 0) (str ":" port) "")]
    (str scheme "://" host port)))

(defn host-name
  "gets the host name part from an url"
  [url]
  (let [x (str/replace (str/lower-case url) #"http(s)?://(www\.)?" "")]
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

(defn string-or
  ([s]
    (string-or s ""))
  ([s default]
    (if (empty? s)
      default
      (str/trim s))))

(defn int-or
  "try to coerce to integer or return a safe default"
  [s default]
  (if (nil? s)
    default
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
      default))))

(defn add-fields [coll]
  (let [info (video-info (:url coll))
        meta-foo (if (= "" (str/trim (:meta coll))) "{}" (:meta coll))
        meta (json/read-str meta-foo :key-fn keyword)
        ;updated (:updated coll);(put-time (read-time (str (:updated coll))))
        prefix (make-url (:static-scheme (config/env :multiplex)) (:static-url (config/env :multiplex)))
        url (if (< (count (:url coll)) (count (config/env :rel-path)))
                ""
                (if (= (config/env :rel-path) (subs (:url coll) 0 (count (config/env :rel-path))))
                    (str prefix (:url coll))
                    (:url coll)))]
    (assoc coll :code (:code info)
                :site (:site info)
                :url url
                :thumb-path (str prefix (config/env :rel-path))
                ;:updated (or updated (:updated coll))
                :thumbnail (:thumbnail meta)
                :meta meta)))

(defn set-author [coll & [request]]
  (let [request (or request {})
        fields  [:username :hostname :title :avatar :theme :is_private :uid]
        author  (select-keys coll fields)
        author  (assoc author :url (make-url (:page-scheme (config/env :multiplex)) (:hostname author) request))]
    (assoc (apply (partial dissoc coll) fields) :author author)))

(defn keywordize [m]
  (zipmap (map keyword (keys m)) (vals m)))

(defn calculate-pagination
  [num page post-count]
  (let [page-count (int (Math/ceil (/ post-count num)))
        page-newer (when-not (< page 2) (dec page))
        page-older (when-not (>= page page-count) (inc page))
        pages (range 1 (inc page-count))]
    {:page-newer page-newer
     :page-older page-older
     :pages pages
     :page-count page-count
     :post-count post-count}))

(defn type-pagination [type page limit]
  (let [pt (str "type=" type)
        pp (when-not (> 1 (int-or page 1)) (str "page=" page))
        pl (when-not (= config/default-limit limit) (str "limit=" limit))
        parts [pt pl pp]]
    (str "?" (clojure.string/join "&" (filter not-empty parts)))))

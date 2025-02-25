(ns multiplex.util
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [clojure.data.json :as json]
    [clojure.string :as cstr]
    [clojure.tools.logging :as log]
    [java-time :as jtime]
    [multiplex.config :as config]
    [ring.util.codec :as rcodec]))


(def config-fallback {:site-title "multiplex"
                      :site-scheme :http
                      :site-url ""
                      :site-port 80
                      :site-theme "default"
                      :assets-url ""})

; format of tags:
; allow either a single alphanumeric char,
; or allow alphanumeric chars with
; _ at the start and _ - after the first char
(def tag-regex #"^[a-z0-9]+|[a-z0-9_]+[a-z0-9_-]+$")

; format of searches
(def search-regex #"^[a-z0-9_-]{3,}$")

(defn string-or
  ([s]
    (string-or s ""))
  ([s default]
    (if (empty? s)
      default
      (cstr/trim s))))

(defn int-or
  "try to coerce to integer or return a safe default"
  [s default]
  (if (nil? s)
    default
  (try
    (let [n (cond
              (instance? java.lang.Integer s) (long s)
              (instance? java.lang.Long s) s
              (instance? java.lang.String s) (Long/parseLong s)
              (instance? java.lang.Double s) (long s)
              :else default)]
      (if (pos? n) n default))
    (catch Exception e
      default))))

(defn is-custom-host [hostname]
  (if (= (:site-url (config/env :multiplex)) hostname)
    false
    hostname))

(defn valid-post-type?
  "determines if the given type of post is allowed"
  [arg]
  (if (empty? arg)
    false
    (some #(= arg %) config/itemtypes)))

(defn valid-tag?
  "determines if the given tag is allowed"
  [arg]
  (re-matches tag-regex (cstr/lower-case arg)))

(defn valid-search?
  "determines if the given search is allowed"
  [arg]
  (re-matches search-regex (cstr/lower-case (cstr/replace arg #"\*" ""))))

(defn file-extension
  "gets the lower-cased file extension from a string"
  [name]
  (if (empty? name)
    ""
    (let [parts (cstr/split name #"\.")
          ext (re-find #"[a-z0-9]+" (cstr/lower-case (last parts)))]
      (cond
        (< (count parts) 2) "" ; "foo"
        (and (= 2 (count parts)) (= "" (first parts))) "" ; ".foo"
        (= "jpeg" ext) "jpg"
        :else ext))))

(defn make-url [host config]
  (let [cfs (:site-scheme config)
        scheme (or (if (keyword? cfs) cfs (keyword (cstr/replace cfs ":" ""))) :http)
        port (or (int-or (:site-port config) 0) (if (= :https scheme) 443 80))
        suffix (if (pos? port) (str ":" port) "")]
    (cond
      (and (= :http scheme) (= 80 port)) (str (name scheme) "://" host)
      (and (= :https scheme) (= 443 port)) (str (name scheme) "://" host)
      :else (str (name scheme) "://" host suffix))))

(defn host-config [request]
  (let [cfg (first (remove empty? [(config/env :multiplex) config-fallback]))
        site-url (:site-url cfg)
        hostname (:server-name request)]
    {:hostname hostname
     :base-url (make-url site-url cfg)
     :user-url (make-url hostname cfg)
     :server-port (:server-port request)
     :scheme (:scheme request)}))

(defn host-name
  "gets the host name part from an url"
  [url]
  (let [x (cstr/replace (cstr/lower-case url) #"http(s)?://(www\.)?" "")]
    (first (cstr/split x #"/"))))

(defn video-info
  "extract the unique part of a video url, e.g. from youtube.com/watch?v=FOO"
  [s]
  (let [host (host-name s)
        rv {:url s}]
    (cond
      (some #{host} config/sites-youtube)
        (let [matcher (re-matcher #"[\?&]v=([a-zA-Z0-9_-]+)" s)
              code (second (re-find matcher))]
          (assoc rv :site "youtube" :code code))
      (some #{host} config/sites-vimeo)
        (let [matcher (re-matcher #"/([0-9]+)" s)
              code (second (re-find matcher))]
          (assoc rv :site "vimeo" :code code))
      (some #{host} config/sites-soundcloud)
        (let [path (.getRawPath (java.net.URI. s))]
          (assoc rv :site "soundcloud" :code (rcodec/url-encode path)))
      (some #{host} config/sites-mixcloud)
        (let [path (.getRawPath (java.net.URI. s))]
          (assoc rv :site "mixcloud" :code (rcodec/url-encode path)))
      (and (some #{host} config/sites-imgur-gifv) (cstr/ends-with? s ".gifv"))
        (let [matcher (re-matcher #"https?://[^/]+/(.+)\.gifv$" s)]
          (assoc rv :site "imgur-gifv" :code (second (re-find matcher))))
      :else (assoc rv :site "err" :code ""))))

(defn thumbnail-url
  "get the thumbnail url for a video site"
  [m]
  (cond
    (= "youtube"    (:site m)) (str "https://i.ytimg.com/vi/" (:code m) "/hqdefault.jpg")
    (= "vimeo"      (:site m)) (str "https://i.vimeocdn.com/video/" (:thumb-id m) ".jpg?mw=480")
    (= "soundcloud" (:site m)) (str (:thumb-path m) (:thumb-id m) "." (:thumb-ext m))
    :else ""))

(defn add-fields [coll]
  (let [config (config/env :multiplex)
        prefix (:assets-url config)
        meta-foo (if (= "" (cstr/trim (:meta coll))) "{}" (:meta coll))
        meta (json/read-str meta-foo :key-fn keyword)]
    (assoc coll :code (:code meta)
                :site (:site meta)
                :thumb-path (str prefix (:content-rel-path config))
                :thumbnail (:thumbnail meta)
                :tags (when-not (empty? (:tags coll)) (:tags coll))
                :meta meta)))

(defn convert-time [d]
  (let [d2 (jtime/format "yyyy-MM-dd HH:mm" d)
        d3 (jtime/format (jtime/formatter :iso-instant) (jtime/zoned-date-time d "UTC"))]
    [(or d2 d) d3]))

(defn fix-time-fields
  "formats java.time.LocalDateTime to strings"
  [coll]
  (let [created (convert-time (:created coll))
        updated (convert-time (:updated coll))]
    (assoc coll :created-ts (second created)
                :created (first created)
                :updated-ts (second updated)
                :updated (first updated))))

(defn fix-url-field [coll]
  (let [config (config/env :multiplex)
        prefix (:assets-url config)
        rel-path (:content-rel-path config)
        url (or (:url coll) "")
        url (if (cstr/starts-with? url rel-path)
                (str prefix url)
                url)]
    (assoc coll :url url)))

(defn set-author [coll & [request]]
  (let [request (or request {})
        fields  [:username :hostname :title :avatar :theme :is_private :uid]
        author  (select-keys coll fields)
        url     (make-url (:hostname author) (config/env :multiplex))
        author  (assoc author :uid (or (:author coll) (:uid coll)) :url url)]
    (assoc (apply (partial dissoc coll) fields) :author author)))

(defn keywordize [m]
  (zipmap (map keyword (keys m)) (vals m)))

(defn calculate-pagination
  [limit page post-count]
  (let [page-count (int (Math/ceil (/ post-count limit)))
        page-newer (when-not (< page 2) (dec page))
        page-older (when-not (>= page page-count) (inc page))
        pages (range 1 (inc page-count))]
    {:page-newer page-newer
     :page page
     :page-older page-older
     :pages pages
     :limit limit
     :page-count page-count
     :post-count post-count}))

(defn type-pagination [type page limit]
  (let [pt (str "type=" type)
        pp (when     (< 1 (int-or page 1)) (str "page=" page))
        pl (when-not (= config/default-limit limit) (str "limit=" limit))
        parts [pt pl pp]]
    (str "?" (cstr/join "&" (remove empty? parts)))))

(defn sanitize-title [s]
  (-> s
      (cstr/replace #"^▶ " "")
      (cstr/replace #" - YouTube$" "")
      (cstr/replace #" - MyVideo$" "")
      (cstr/replace #" \| Mixcloud$" "")
      (cstr/replace #" on Vimeo$" "")))

(defn guess-type
  "if the post type is not given, try guessing from contents"
  [url txt]
  (if (empty? url)
    "text"
    (let [ext (file-extension url)
          host (host-name url)]
      (cond
        (some #{host} config/sites-video) "video"
        (some #{host} config/sites-audio) "audio"
        (some #{ext} config/img-types) "image"
        :else "link"))))

(defn hash-filename [arg]
  (codecs/bytes->hex (hash/sha1 arg)))

(defn join-params [kv]
  (cstr/join (apply concat
    (zipmap
      (map #(str "&" (name %) "=") (keys kv))
      (map rcodec/url-encode (vals kv))))))

(defn unwrap-tags [coll]
  (assoc coll :tags (cstr/join "," (:tags coll))))

(defn sanitize-tags [s]
  (->> (cstr/split s #",")
       (map cstr/lower-case)
       (filter #(re-matches tag-regex %))
       (remove empty?)))

(defn get-filename [url]
  (let [path (.getRawPath (java.net.URI. url))
        parts (cstr/split path #"/")]
    (or (last parts) "")))

(defn audit [x & [xs]]
  (let [msg (str "event." (cstr/replace (name x) #"-" "."))]
    (log/info msg xs)
    [msg xs]))

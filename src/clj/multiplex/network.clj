(ns multiplex.network
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as cjio]
    [clojure.string :as cstr]))

(defn download-file
  "copies an image from an URL to a local file"
  [url filename]
  (let [parent (.getParent (java.io.File. filename))
        md (.mkdirs (java.io.File. parent))]
    (with-open [input (cjio/input-stream url)
                output (cjio/output-stream filename)]
      (cjio/copy input output))))

(defn read-remote
  "read from remote url"
  [url default]
  (try
    (slurp url)
    (catch Exception ex default)))

(defn video-details-vimeo [coll]
  (let [url (str "https://vimeo.com/api/oembed.json?url=https%3A//vimeo.com/" (:code coll))
        json (read-remote url "{\"thumbnail_url\":\"\"}")
        data (json/read-str json :key-fn keyword)]
    (assoc coll :thumbnail-url (:thumbnail_url data) :duration (:duration data)
                :title (:title data)
                :artist (:author_name data)
                :thumb-width (:thumbnail_width data) :thumb-height (:thumbnail_height data))))

(defn video-details-soundcloud [coll]
  (let [url (str "https://soundcloud.com/oembed?format=json&url=https%3A//soundcloud.com" (:code coll))
        json (read-remote url "{\"thumbnail_url\":\"\"}")
        data (json/read-str json :key-fn keyword)
        matcher (re-matcher #"api.soundcloud.com%2Ftracks%2F([0-9]+)&?" (:html data))]
    (assoc coll :thumbnail-url (:thumbnail_url data)
                :title (:title data)
                :artist (:author_name data)
                :id (second (or (re-find matcher) [0 0])))))

(defn video-details-mixcloud [coll]
  (let [url (str "https://www.mixcloud.com/oembed?format=json&url=https%3A//www.mixcloud.com" (:code coll))
        json (read-remote url "{\"image\":\"\"}")
        data (json/read-str json :key-fn keyword)]
    (assoc coll :thumbnail-url (:image data)
                :title (:title data)
                :artist (:author_name data))))

(defn video-details-soundcloud2 [coll]
  (let [html (read-remote (:url coll) "")
        matcher1 (re-matcher #"content=\"soundcloud://sounds:([0-9]+)\"" html)
        matcher2 (re-matcher #"og:image\" content=\"([^\"]+)\"" html)
        img (or (second (re-find matcher2)) "")
        ;ext (util/file-extension img)
        ext (last (cstr/split img #"/"))
        parts (cstr/split img #"/")]
    (if-let [code (second (re-find matcher1))]
      (assoc coll :code code
                  :thumb-id (cstr/replace (last parts) (str "." ext) "")
                  :thumb-path (cstr/replace img (last parts) "")
                  :thumb-ext ext)
      coll)))

(defn video-details [coll]
  (cond
    (= "vimeo"      (:site coll)) (video-details-vimeo coll)
    (= "soundcloud" (:site coll)) (video-details-soundcloud coll)
    (= "mixcloud"   (:site coll)) (video-details-mixcloud coll)
    :else coll))
(ns multiplex.db.posts
  (:require
   [clojure.data.json :as json]
   [clojure.string :as cstr]
   [multiplex.config :as config]
   [multiplex.gfx :as gfx]
   [multiplex.util :as util]
   [multiplex.db.core :as db]))

(defn store-image [params]
  (let [orig-url (:url params)
        ext (util/file-extension orig-url)
        filename (str (util/hash-filename orig-url) "." ext)
        abs-filename (config/abs-file filename)
        ximg (util/download-file orig-url abs-filename)
        img (gfx/read-image abs-filename)
        sizes (gfx/image-size img)
        resized (gfx/calc-resized img)
        params (assoc params :meta {:size (cstr/join ":" sizes) :url orig-url}
                             :url (config/rel-file filename)
                             :author (util/int-or (:author params) 0))]
      (println "DEBUG store-image: " params)
      (if-let [need (gfx/needs-resize? sizes resized abs-filename)]
        (let [rv (gfx/resize img abs-filename (first resized) (second resized))
              meta (assoc (:meta params) :thumb (util/file-extension abs-filename) :thumbsize (cstr/join ":" resized))
              params (assoc params :meta (json/write-str meta))]
          (db/create-post! params))
        (db/create-post! params))))

(defn store-video-thumb [params]
  (let [vi       (util/video-info (:url params))
        thumb    (util/thumbnail-url vi)
        ext      (util/file-extension thumb)
        filename (str (:thumb-id vi) "." ext)
        abs-file (config/abs-file-thumb filename (:site vi))
        ximg     (util/download-file thumb abs-file)
        img      (gfx/read-image abs-file)
        resized  (gfx/calc-resized img)
        meta     (assoc vi :thumbnail filename
                           :thumbsize (cstr/join ":" resized))]
    (println "DEBUG store-video-thumb: " params)
    (db/create-post! (assoc params :meta meta))))

(defn store-rest [params]
  (let [params (assoc params :meta "{}")]
    (println "DEBUG store-rest: " params)
    (db/create-post! params)))

(defn store-dispatch [params]
  (cond
    (= "video" (:itemtype params)) (store-video-thumb params)
    (= "audio" (:itemtype params)) (store-video-thumb params)
    (= "image" (:itemtype params)) (store-image params)
    (= "text" (:itemtype params))  (store-rest params)
    :else                          (store-rest params)))

(defn create-post! [params]
  (println "dbp/create-post!" params)
  (let [itemtype (if (empty? (:itemtype params)) (util/guess-type (:url params) (:txt params)) (:itemtype params))
        tags (util/sanitize-tags (:tags params))
        jtags (str "'" (json/write-str tags) "'::json")
        jtags2 (str "to_json(string_to_array('" (cstr/join "," tags) "',','))")
        xx (println "dbp/create-post!" jtags)
        params (assoc params :itemtype itemtype :tags_raw jtags2 :tag (cstr/join "," tags))
        newid (store-dispatch params)]
        (println "newid" newid)
    newid))

(defn update-post! [params]
  (let [tags (util/sanitize-tags (:tags params))
        params (assoc params :id (util/int-or (:id params) 0) :tags_raw (json/write-str tags) :tag (cstr/join ","))]
    (println "dbp/update-post!" params)
    (db/update-post! params)))

(defn delete-post! [params]
  (let [crit {:id (util/int-or (:id params) 0)}]
    (println "dbp/delete-post!" crit)
    (db/delete-post! crit)))

(defn get-some-posts [crit]
  (println "get-some-posts" crit)
  (let [posts (db/get-post-by-id crit)]
    (cond
      (pos? (:id crit)) [(count posts) posts]
      :else             [(:count (db/get-posts-filtered-count crit)) (db/get-posts-filtered crit)])))

(defn get-posts [what params & [request]]
  (let [params   (or params {})
        ; sanitize user input
        itemtype (util/string-or (get params :type))
        tag      (util/string-or (get params :tag))
        limit    (util/int-or (get params :limit) config/default-limit)
        page     (util/int-or (get params :page) 1)
        author   (util/int-or (get params :author) 0)
        id       (util/int-or (get params :id) 0)
        offset   (* limit (dec page))
        ; check user input to disallow sql injection
        filter   (if (= :some what)            (str filter " AND p.author = " author) "")
        filter   (if (util/valid-post-type? itemtype) (str " AND p.itemtype = '" itemtype "'") filter)
        filter   (if (util/valid-tag? tag)     (str filter " AND p.tags @> '\"" tag "\"'") filter)
        ; build WHERE criteria
        crit     {:limit limit :offset offset :author author :id id :posts_crit_raw filter}
        crit     (if (util/valid-post-type? itemtype) (assoc crit :itemtype itemtype) crit)
        crit     (if (util/valid-tag? tag) (assoc crit :tag tag) crit)
        orig     (get-some-posts crit)]
    [(first orig) (->> (second orig)
         (map #(util/add-fields %))
         (map #(util/set-author % request)))]))
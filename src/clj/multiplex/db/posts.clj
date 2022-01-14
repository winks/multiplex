(ns multiplex.db.posts
  (:require
   [clojure.data.json :as json]
   [clojure.string :as cstr]
   [clojure.tools.logging :as log]
   [multiplex.config :as config]
   [multiplex.gfx :as gfx]
   [multiplex.network :as net]
   [multiplex.util :as util]
   [multiplex.db.core :as db]))

(def post-fields [:url :txt :tags :author :id])

(defn store-image [params]
  (let [orig-url (:url params)
        ext      (or (util/file-extension orig-url) "png")
        filename (str (util/hash-filename orig-url) "." ext)
        abs-filename (config/abs-file filename)
        ximg     (net/download-file orig-url abs-filename)
        img      (gfx/read-image abs-filename)
        sizes    (gfx/image-size img)
        resized  (gfx/calc-resized img)
        params   (assoc params :meta {:size (cstr/join ":" sizes) :url orig-url}
                               :url (config/rel-file filename)
                               :author (util/int-or (:author params) 0))]
      (log/debug "store-image: " params)
      (if-let [need (gfx/needs-resize? sizes resized abs-filename)]
        (let [rv (gfx/resize img abs-filename (first resized) (second resized))
              meta (assoc (:meta params) :thumb ext :thumbsize (cstr/join ":" resized))
              params (assoc params :meta (json/write-str meta))]
          (db/create-post! params))
        (db/create-post! params))))

(defn store-video-thumb [params]
  (let [vi (util/video-info (:url params))
        vi (net/video-details vi)
        tu (or (:thumbnail-url vi) (util/thumbnail-url vi))
        vi (assoc vi :thumbnail-url tu)]
    (if-let [filename (util/get-filename (:thumbnail-url vi))]
      (let [ext      (util/file-extension filename)
            ext      (if (and (empty? ext) (= "vimeo" (:site vi))) "jpg" ext)
            ext      (if (and (empty? ext) (= "mixcloud" (:site vi))) "jpg" ext)
            ext      (or ext "png")
            new-name (str (:code vi) "." ext)
            abs-file (config/abs-file-thumb new-name (:site vi))
            ximg     (net/download-file (:thumbnail-url vi) abs-file)
            img      (gfx/read-image abs-file)
            resized  (gfx/calc-resized img)
            ;title    (if (= "soundcloud" (:site vi)) (:title vi) nil)
            ;title    (or title (:title params))
            meta     (assoc vi :thumbnail (util/get-filename abs-file)
                               :thumbsize (cstr/join ":" resized))]
        (log/debug "store-video-thumb: " params)
        (db/create-post! (assoc params :meta meta)))
      (log/info "store-video-thumb failed: " params))))

(defn store-rest [params]
  (let [params (assoc params :meta "{}")]
    (log/debug "store-rest: " params)
    (db/create-post! params)))

(defn store-dispatch [params]
  (cond
    (= "video" (:itemtype params)) (store-video-thumb params)
    (= "audio" (:itemtype params)) (store-video-thumb params)
    (= "image" (:itemtype params)) (store-image params)
    (= "text"  (:itemtype params)) (store-rest params)
    :else                          (store-rest params)))

(defn create-post! [params]
  (log/debug "dbp/create-post!" params)
  (let [params (select-keys (or params {}) post-fields)
        author (util/int-or (:author params) 0)
        url (:url params)
        txt (:txt params)
        tags (util/sanitize-tags (util/string-or (:tags params)))
        tagstring (cstr/join "," tags)
        jtags (str "to_json(string_to_array('" tagstring "', ','))")
        itemtype (util/guess-type url txt)
        crit {:itemtype itemtype :author author :tag tagstring :tags_raw jtags :url url :txt txt}
        newid (store-dispatch crit)]
        (log/debug "newid" newid)
    newid))

(defn update-post! [params orig]
  (let [params (select-keys (or params {}) post-fields)
        id (util/int-or (:id params) 0)
        author (util/int-or (:author params) 0)
        url (:url params)
        txt (:txt params)
        tags (util/sanitize-tags (util/string-or (:tags params)))
        tagstring (cstr/join "," tags)
        jtags (str "to_json(string_to_array('" tagstring "', ','))")
        crit {:id id :author author :tag tagstring :tags_raw jtags :url url :txt txt}]
    (log/debug "dbp/update-post!" crit)
    (db/update-post! crit)))

(defn delete-post! [params]
  (let [crit {:id (util/int-or (:id params) 0)}]
    (log/debug "dbp/delete-post!" crit)
    (db/delete-post! crit)))

(defn get-some-posts [crit]
  (log/debug "get-some-posts" crit)
  (log/debug "get-some-posts" crit)
  (cond
    (pos? (:id crit))
      (let [posts (db/get-post-by-id crit)]
        [(count posts) posts])
    :else (let [num (db/get-posts-filtered-count crit)
                posts (db/get-posts-filtered crit)]
            [(:count num) posts])))

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
        filter   (if (= :some what)                          (str " AND p.author = " author) "")
        filter   (if (util/valid-post-type? itemtype) (str filter " AND p.itemtype = '" itemtype "'") filter)
        filter   (if (util/valid-tag? tag)            (str filter " AND p.tags @> '\"" tag "\"'") filter)
        ; build WHERE criteria
        crit     {:limit limit :offset offset :author author :id id :posts_crit_raw filter}
        crit     (if (util/valid-post-type? itemtype) (assoc crit :itemtype itemtype) crit)
        crit     (if (util/valid-tag? tag) (assoc crit :tag tag) crit)
        orig     (get-some-posts crit)]
    [(first orig) (->> (second orig)
         (map #(util/add-fields %))
         (map #(util/fix-time-fields %))
         (map #(util/fix-url-field %))
         (map #(util/set-author % request)))]))
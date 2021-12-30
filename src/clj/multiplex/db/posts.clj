(ns multiplex.db.posts
  (:require
   [multiplex.config :as config]
   [multiplex.util :as util]
   [multiplex.db.core :as db]))

(comment
(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-filename (:url params)) "." ext)
        abs-filename (config/abs-file filename)
        x (util/download-file (:url params) abs-filename)
        img (gfx/read-image abs-filename)
        sizes (gfx/image-size img)
        resized (gfx/calc-resized img)
        params (assoc params :id nil
                             :meta {:size (str/join ":" sizes) :url (:url params)}
                             :url (config/rel-file filename))]
    (do
      (println (str "DEBUG store-image: " (:url params) sizes resized))
      (if-let [need (gfx/needs-resize? sizes resized abs-filename)]
        (let [r (gfx/resize img abs-filename (first resized) (second resized))
              params (assoc params :meta (assoc (:meta params) :thumb (util/file-extension abs-filename)
                                                               :thumbsize (str/join ":" resized)))]
          (prep-new params))
        (prep-new params)))))

(defn store-video-thumb [params]
  (let [vi       (util/video-info (:url params))
        thumb    (util/thumbnail-url vi)
        ext      (util/file-extension thumb)
        filename (str (:thumb-id vi) "." ext)
        abs-file (config/abs-file-thumb filename (:site vi))
        x        (util/download-file thumb abs-file)
        img      (gfx/read-image abs-file)
        resized  (gfx/calc-resized img)
        params   (assoc params :meta (assoc vi :thumbnail filename
                                               :thumbsize (str/join ":" resized)))]
    (do
      (println (str "DEBUG store-video-thumb: " params))
      (prep-new params))))
)

(defn store-rest [params]
  (let [params (assoc params ; :id nil ???
                             :meta "{}")]
    (println (str "DEBUG store-rest: " params))
    (db/create-post! params)))

(defn store-dispatch [params]
  (cond
    ;(= "video" (:itemtype params)) (store-video-thumb params)
    ;(= "audio" (:itemtype params)) (store-video-thumb params)
    ;(= "image" (:itemtype params)) (store-image params)
    (= "text" (:itemtype params))  (store-rest params)
    :else                          (store-rest params)))

(defn create-post! [params]
  (println "dbp/create-post!" params)
  (let [itemtype (if (empty? (:itemtype params)) (util/guess-type (:url params) (:txt params)) (:itemtype params))
        params (assoc params :itemtype itemtype)
        newid (store-dispatch params)]
        (println "newid" newid)
    newid))

(defn get-some-posts [params crit]
  (println "get-some-posts" crit)
  (let [id       (util/int-or (get params :id) 0)
        crit     (assoc crit :author (:author params) :id id)]
    (cond
      (> id 0)                  [1 (db/get-post crit)]
      (empty? (:itemtype crit)) [(:count (db/get-posts-count crit)) (db/get-posts crit)]
      :else                     [(:count (db/get-posts-filtered-count crit)) (db/get-posts-filtered crit)])))

(defn get-all-posts [params crit]
  (println "get-all-posts" crit)
  (cond
    (empty? (:itemtype crit)) [(:count (db/get-all-posts-count crit)) (db/get-all-posts crit)]
    :else                     [(:count (db/get-all-posts-filtered-count crit)) (db/get-all-posts-filtered crit)]))

(defn get-posts [what params & [request]]
  (let [params   (or params {})
        itemtype (util/string-or (get params :type))
        limit    (util/int-or (get params :limit) config/default-limit)
        page     (util/int-or (get params :page) 1)
        offset   (* limit (dec page))
        crit     {:limit limit :offset offset}
        crit     (if (util/valid-post-type? itemtype) (assoc crit :itemtype itemtype) crit)
        orig     (cond
                   (= :some what) (get-some-posts params crit)
                   :else          (get-all-posts params crit))]
    [(first orig) (->> (second orig)
         (map #(util/add-fields %))
         (map #(util/set-author % request)))]))
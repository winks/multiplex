(ns multiplex.db.posts
  (:require
   [multiplex.config :as config]
   [multiplex.util :as util]
   [multiplex.db.core :as db]))

(defn get-some-posts [params crit]
  (println "get-some-posts")
  (let [id       (util/int-or (get params :id) 0)
        crit     (assoc crit :author (:author params) :id id)]
    (cond
      (> id 0) (db/get-post crit)
      (empty? (:itemtype crit)) (db/get-posts crit)
      :else (db/get-posts-filtered crit))))

(defn get-all-posts [params crit]
  (println "get-all-posts")
  (cond
    (empty? (:itemtype crit)) (db/get-all-posts crit)
    :else (db/get-all-posts-filtered crit)))

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
                   :else (get-all-posts params crit))]
    (->> orig
         (map #(util/add-fields %))
         (map #(util/set-author % request)))))
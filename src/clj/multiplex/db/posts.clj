(ns multiplex.db.posts
  (:require
   [multiplex.config :as config]
   [multiplex.util :as util]
   [multiplex.db.core :as db]))

(defn get-posts [params & [request]]
  (let [params   (or params {})
        ;author   (or author {})
        itemtype (util/string-or-default (get params :itemtype))
        limit    (util/int-or-default (get params :limit) config/default-limit)
        page     (util/int-or-default (get params :page) 1)
        id       (util/int-or-default (get params :id) 0)
        offset   (* limit (dec page))
        crit     {:author (:author params) :limit limit :offset offset :id id}
        crit     (if (util/valid-post-type? itemtype) (assoc crit :itemtype itemtype) crit)
        orig     (cond
                   (> id 0) (db/get-post crit)
                   (empty? itemtype) (db/get-posts crit)
                   :else (db/get-posts-filtered crit))]
    (->> orig
         (map #(util/add-fields %))
         (map #(util/set-author % request)))))

(defn get-some-posts [params & [request]] nil)

(defn get-all-posts [params & [request]]
  (let [params   (or params {})
        itemtype (util/string-or-default (get params :itemtype))
        limit    (util/int-or-default (get params :limit) config/default-limit)
        page     (util/int-or-default (get params :page) 1)
        offset   (* limit (dec page))
        crit     {:limit limit :offset offset}
        crit     (if (util/valid-post-type? itemtype) (assoc crit :itemtype itemtype) crit)
        orig     (cond
                   (empty? itemtype) (db/get-all-posts crit)
                   :else (db/get-all-posts-filtered crit))]
    (->> orig
         (map #(util/add-fields %))
         (map #(util/set-author % request)))))
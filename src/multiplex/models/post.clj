(ns multiplex.models.post
  (:use korma.core
        [korma.db :only (defdb mysql postgres)])
  (:require [multiplex.config :as config]
            [multiplex.models.db :as db]))

(def itemtypes ["image" "video" "audio" "link" "text"])

(defn valid-itemtype? [what] (some #{what} itemtypes))

; SQLish
(defn get-post-count [where-clause]
  (let [query (-> (select* db/mpx_posts)
                  (aggregate (count :id) :cnt))]
    (if (= {} where-clause)
      (:cnt (first (-> query (select))))
      (:cnt (first (-> query (where where-clause) (select)))))))

(defn get-post-by-id [id]
  (do
    (println (str id ":" (class id)))
    (println (sql-only
      (select db/mpx_posts
        (where {:id id})
        (join db/mpx_users (= :mpx_users.uid :author))
        (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username]))))
    (select db/mpx_posts
      (where {:id id})
      (join db/mpx_users (= :mpx_users.uid :author))
      (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username]))))

(defn get-posts
  ([n]
    (get-posts n 0 {}))
  ([n off]
    (get-posts n off {}))
  ([n off where-clause]
    (let [q (-> (select* db/mpx_posts)
                (join db/mpx_users (= :mpx_users.uid :author))
                (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username])
                (order :id :DESC)
                (limit n)
                (offset off))]
      (if (= {} where-clause)
        (do
          (println (-> q (as-sql)))
          (-> q (select)))
        (do
          (println (-> q (where where-clause) (as-sql)))
          (-> q (where where-clause) (select)))))))

(defn new-post [params]
  (do
    (println params)
    (println (sql-only (insert db/mpx_posts (values params))))
    (insert db/mpx_posts (values params))))

(ns multiplex.models.post
  (:require [clojure.java.jdbc :as sql]
            [korma.core :refer :all]
            [multiplex.config :as config]
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
    (println (sql-only
      (select db/mpx_posts
        (where {:id id})
        (join db/mpx_users (= :mpx_users.uid :author))
        (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username] [:mpx_users.hostname :hostname] [:mpx_users.avatar :avatar]))))
    (select db/mpx_posts
      (where {:id id})
      (join db/mpx_users (= :mpx_users.uid :author))
      (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username] [:mpx_users.hostname :hostname] [:mpx_users.avatar :avatar]))))

(defn get-posts
  ([n]
    (get-posts n 0 {}))
  ([n off]
    (get-posts n off {}))
  ([n off where-clause]
    (let [q (-> (select* db/mpx_posts)
                (join db/mpx_users (= :mpx_users.uid :author))
                (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:mpx_users.username :username] [:mpx_users.hostname :hostname] [:mpx_users.avatar :avatar])
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
  (let [query (str "INSERT INTO mpx_posts"
                   " (id, updated, created, author, itemtype, url, txt, meta, tag)"
                   " VALUES (nextval('mpx_posts_id_seq'), NOW(), NOW(), ? ,?, ?, ?, ?, ?) RETURNING id;")]
    (sql/query config/mydb [query (:author params) (:itemtype params) (:url params) (:txt params) (:meta params) (:tag params)])))

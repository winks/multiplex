(ns multiplex.models.post
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]
            [multiplex.models.db :as db]))

; SQLish
(defn get-post-count []
  (:cnt (first (select db/clj (aggregate (count :id) :cnt)))))

(defn get-post-by-id [id]
  (do
    (println (sql-only
      (select db/clj
        (where {:id id})
        (join db/users (= :users.uid :author))
        (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:users.username :username]))))
    (select db/clj
      (where {:id id})
      (join db/users (= :users.uid :author))
      (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:users.username :username]))))

(defn get-posts
  ([n]
    (get-posts n 0))
  ([n off]
    (do
      (println (sql-only
        (select db/clj
          (join db/users (= :users.uid :author))
          (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:users.username :username])
          (order :id :DESC)
          (limit n)
          (offset off))))
      (select db/clj
        (join db/users (= :users.uid :author))
        (fields :id :author :itemtype :url :txt :meta :tag :created :updated [:users.username :username])
        (order :id :DESC)
        (limit n)
        (offset off)))))

(defn new-post [params]
  (do
    (println params)
    (println (sql-only (insert db/clj (values params))))
    (insert db/clj (values params))))

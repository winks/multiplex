(ns multiplex.models.post
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]
            [multiplex.models.db :as db]))

(defentity clj)

; SQLish
(defn get-post-count []
  (:cnt (first (select clj (aggregate (count :id) :cnt)))))

(defn get-post-by-id [id]
  (do
    (println (sql-only (select clj (where {:id id}))))
    (select clj (where {:id id}))))

(defn get-posts
  ([n]
    (get-posts n 0))
  ([n off]
    (do
      (println (sql-only
        (select clj
                (order :id :DESC)
                (limit n)
                (offset off))))
      (select clj
            (order :id :DESC)
            (limit n)
            (offset off)))))

(defn new-post [params]
  (do
    (println params)
    (println (sql-only (insert clj (values params))))
    (insert clj (values params))))

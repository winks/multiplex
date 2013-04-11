(ns simplex-clj.models.db
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [simplex-clj.models.schema :as schema]))

(defdb dbm (mysql
  {:db "simplex"
   :user "simplex"
   :password "simplex"
   :delimiters "`"}))

(defentity users)
(defentity clj)

; users
(defn create-user [user]
  (insert users
          (values user)))

(defn update-user [id first-name last-name email]
  (update users
  (set-fields {:first_name first-name
               :last_name last-name
               :email email})
  (where {:id id})))

(defn get-user [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))
; posts
(defn get-post [id]
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

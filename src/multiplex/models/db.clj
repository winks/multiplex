(ns multiplex.models.db
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]))

(defn convert-db-uri
  "convert a JDBC/Heroku DB-DSN/URI to parts"
  [db-uri]
  (let [[_ user password host port db x y] (re-matches #"mysql://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)(?:\?(.+)=(.+))?" db-uri)]
  (println (str user ":" password "@" host "/" db " " x y))
    {
      :user user
      :password password
      :host host
      :db db
      :delimiters "`"
      (keyword x) y
    }))
;      :port (or port 80)

(defdb dbm (mysql(convert-db-uri config/mydb)))

(defentity clj)
(defentity tags)
(defentity users)

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

(defn get-user-by-id
  [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))

(defn get-user-by-key
  [apikey]
  (first (select users
                 (where {:apikey apikey})
                 (limit 1))))

(defn valid-apikey?
  [apikey]
  (not (nil? (get-user-by-key apikey))))

; posts
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
    (println (sql-only
      (insert clj
        (values params))))
    (insert clj
      (values params))))

(ns multiplex.models.db
  (:require [korma.core :refer :all]
            [korma.db :as kdb]
            [multiplex.config :as config]))

(defn convert-db-uri
  "convert a JDBC/Heroku DB-DSN/URI to parts"
  [db-uri]
  (let [[_ type user password host port db x y] (re-matches #"(mysql|postgres)://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)(?:\?(.+)=(.+))?" db-uri)]
  (println (str "Connecting to " type " DB: " user ":" "XXX@" host "/" db " " x y))
    {
      :user user
      :type type
      :password password
      :host host
      :db db
      :delimiters "`"
      (keyword x) y
    }))

(kdb/defdb dbm
  (let [db-info (convert-db-uri config/mydb)]
    (if (= "postgres" (:type db-info))
      (kdb/postgres (assoc db-info :delimiters ""))
      (kdb/mysql db-info))))

(defentity mpx_posts)
(defentity mpx_tags)
(defentity mpx_users)

(def posts-table "mpx_posts")
(def tags-table  "mpx_tags")
(def users-table "mpx_users")

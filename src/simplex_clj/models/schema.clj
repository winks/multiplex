(ns simplex-clj.models.schema
  (:use     [korma.db :only (defdb mysql)])
  (:require [clojure.java.jdbc :as sql]
            [simplex-clj.util :as util]))

(def db-spec (mysql
  {:classname "com.mysql.jdbc.Driver"
   :subprotocol "mysql"
   :subname "//localhost/simplex"
   :user "simplex"
   :password "simplex"
   :delimiters "`"}))

(defn initialized? []
  (throw (new Exception "TODO: init")))

(defn create-users-table-x
  []
  (sql/with-connection (System/getenv "DATABASE_URL")
    (sql/create-table
      :users
      [:id "varchar(20) PRIMARY KEY"]
      [:first_name "varchar(30)"]
      [:last_name "varchar(30)"]
      [:email "varchar(30)"]
      [:admin :boolean]
      [:last_login :time]
      [:is_active :boolean]
      [:pass "varchar(100)"])))

(defn create-posts-table-x
  []
   (sql/with-connection db-spec
    (sql/create-table
      :clj2
      [:id :integer "unsigned" "PRIMARY KEY" "AUTO_INCREMENT"]
      [:author "varchar(100) DEFAULT NULL"]
      [:itemtype "varchar(100) DEFAULT NULL"]
      [:url "TEXT NOT NULL"]
      [:txt "TEXT NOT NULL"]
      [:meta "TEXT NOT NULL"]
      [:tag "text NOT NULL"]
      [:created "timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'"]
      [:updated "timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"])))

(defn create-posts-table
  [db-cred]
  (sql/with-connection db-cred
    (sql/do-commands
      (str "CREATE TABLE clj2 (`id` int(11) unsigned NOT NULL AUTO_INCREMENT, "
        "`author` varchar(100) DEFAULT NULL, "
        "`itemtype` varchar(100) DEFAULT NULL, "
        "`url` text NOT NULL, "
        "`txt` text NOT NULL, "
        "`meta` text NOT NULL, "
        "`tag` text NOT NULL, "
        "`created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00', "
        "`updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
        "PRIMARY KEY(`id`)) "
        "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"))))

(defn create-tables
  "creates the database tables used by the application"
  []
  (do
    (create-users-table-x)
    (create-posts-table-x)))

(defn get-post
  ([id]
    (sql/with-connection db-spec
      (sql/with-query-results
        res ["select * from clj where id = ?" id] (first res))))
  ([id post-type]
    (if
      (util/valid-post-type? post-type)
      (sql/with-connection db-spec
        (sql/with-query-results
          res ["select * from clj where id = ? and itemtype = ?" id post-type] (first res)))
      (get-post id))))

(defn get-posts2
  [n]
  (sql/with-connection db-spec
    (sql/with-query-results
      res ["select * from clj order by id DESC LIMIT ?" n] (doall res))))

(defn new-post [params]
  (sql/with-connection db-spec
    (sql/insert-record :clj params)))

(defn -main []
  (print "Creating DB structure...") (flush)
  (create-posts-table (System/getenv "DATABASE_URL")
  (println " done"))

(ns simplex-clj.models.schema
  (:use     [korma.db :only (defdb mysql)])
  (:require [clojure.java.jdbc :as sql]
            [simplex-clj.util :as util]))

(defn create-posts-table
  [db-cred]
  (sql/with-connection db-cred
    (sql/do-commands
      (str "CREATE TABLE clj (`id` int(11) unsigned NOT NULL AUTO_INCREMENT, "
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

(defn -main []
  (print "Creating DB structure...") (flush)
  (create-posts-table (System/getenv "CLEARDB_DATABASE_URL"))
  (println " done"))

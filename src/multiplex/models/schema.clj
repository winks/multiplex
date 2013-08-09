(ns multiplex.models.schema
  (:use     [korma.db :only (defdb mysql)])
  (:require [clojure.java.jdbc :as sql]
            [multiplex.config :as config]
            [multiplex.util :as util]))

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

(defn create-users-table
  [db-cred]
  (sql/with-connection db-cred
    (sql/do-commands
      (str "CREATE TABLE `users` (`uid` int(10) unsigned NOT NULL AUTO_INCREMENT, "
        "`username` varchar(42) COLLATE utf8_unicode_ci NOT NULL, "
        "`email` varchar(100) COLLATE utf8_unicode_ci NOT NULL, "
        "`password` varchar(128) COLLATE utf8_unicode_ci NOT NULL, "
        "`apikey` varchar(64) COLLATE utf8_unicode_ci NOT NULL, "
        "`signupcode` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL, "
        "`created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00', "
        "`updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
        "PRIMARY KEY (`uid`), "
        "UNIQUE KEY `username` (`username`), "
        "UNIQUE KEY `email` (`email`), "
        "UNIQUE KEY `apikey` (`apikey`)) "
        "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"))))

(defn -main []
  (print "Creating DB structure...") (flush)
  (create-posts-table config/mydb)
  (println " done"))

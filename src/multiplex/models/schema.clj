(ns multiplex.models.schema
  (:require [clojure.java.jdbc :as sql]
            [multiplex.config :refer (mydb)]
            [multiplex.models.db :refer (posts-table tags-table users-table)]
            [multiplex.models.load :as mload]
            [multiplex.util :as util]))

(defn create-posts-table
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "CREATE SEQUENCE " posts-table "_id_seq"
               "    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;")
          (str "CREATE TABLE " posts-table " ("
            "id integer NOT NULL PRIMARY KEY DEFAULT nextval('" posts-table "_id_seq'), "
            "author integer NOT NULL, "
            "itemtype character varying(100) DEFAULT NULL::character varying, "
            "url text NOT NULL, "
            "txt text NOT NULL, "
            "meta text NOT NULL, "
            "tag text NOT NULL, "
            "created timestamp(0) without time zone DEFAULT now() NOT NULL, "
            "updated timestamp(0) without time zone NOT NULL"
            ")"))
        (sql/db-do-commands conn
          (str "CREATE TABLE " posts-table " (`id` int(11) unsigned NOT NULL AUTO_INCREMENT, "
            "`author` int(11) unsigned NOT NULL, "
            "`itemtype` varchar(100) DEFAULT NULL, "
            "`url` text NOT NULL, "
            "`txt` text NOT NULL, "
            "`meta` text NOT NULL, "
            "`tag` text NOT NULL, "
            "`created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00', "
            "`updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
            "PRIMARY KEY(`id`)) "
            "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"))))
     (catch Exception e (.getNextException e))))

(defn create-users-table
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "CREATE SEQUENCE " users-table "_id_seq"
               "    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;")
          (str "CREATE TABLE " users-table " ("
            "uid integer NOT NULL PRIMARY KEY DEFAULT nextval('" users-table "_id_seq'), "
            "username character varying(100) NOT NULL UNIQUE, "
            "hostname character varying(100) NOT NULL UNIQUE, "
            "email character varying(100) NOT NULL UNIQUE, "
            "password character varying(128) NOT NULL, "
            "apikey character varying(64) NOT NULL UNIQUE, "
            "signupcode character varying(64) DEFAULT NULL::character varying, "
            "created timestamp(0) without time zone DEFAULT now(), "
            "updated timestamp(0) without time zone NOT NULL,"
            "title character varying(64) NOT NULL default '',"
            "theme text NOT NULL default '')"))
        (sql/db-do-commands conn
          (str "CREATE TABLE `" users-table "` (`uid` int(10) unsigned NOT NULL AUTO_INCREMENT, "
            "`username` varchar(100) COLLATE utf8_unicode_ci NOT NULL, "
            "`hostname` varchar(100) COLLATE utf8_unicode_ci NOT NULL, "
            "`email` varchar(100) COLLATE utf8_unicode_ci NOT NULL, "
            "`password` varchar(128) COLLATE utf8_unicode_ci NOT NULL, "
            "`apikey` varchar(64) COLLATE utf8_unicode_ci NOT NULL, "
            "`signupcode` varchar(64) COLLATE utf8_unicode_ci DEFAULT NULL, "
            "`created` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00', "
            "`updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
            "`title` varchar(64) COLLATE utf8_unicode_ci NOT NULL DEFAULT '', "
            "`theme` TEXT COLLATE utf8_unicode_ci NOT NULL DEFAULT '', "
            "PRIMARY KEY (`uid`), "
            "UNIQUE KEY `username` (`username`), "
            "UNIQUE KEY `email` (`email`), "
            "UNIQUE KEY `apikey` (`apikey`)) "
            "ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;"))))
     (catch Exception e (.getNextException e))))

(defn migrate-users-table-1
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "ALTER TABLE " users-table " ADD COLUMN private boolean NOT NULL DEFAULT '0';" ))
        (sql/db-do-commands conn
          (str "ALTER TABLE `" users-table "` ADD COLUMN private tinyint NOT NULL DEFAULT '0';"))))
     (catch Exception e (.getNextException e))))

(defn migrate-users-table-2
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "ALTER TABLE " users-table " ADD COLUMN avatar character varying(255) NOT NULL DEFAULT '/img/default-avatar.png';" ))
        (sql/db-do-commands conn
          (str "ALTER TABLE `" users-table "` ADD COLUMN avatar varchar(255) NOT NULL DEFAULT '/img/default-avatar.png';"))))
     (catch Exception e (.getNextException e))))

(defn create-functions-etc
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "CREATE FUNCTION update_updated_column() RETURNS trigger "
               "    LANGUAGE plpgsql "
               "    AS $$ "
               "BEGIN "
               "   NEW.updated= now(); "
               "   RETURN NEW; "
               "END; "
               "$$;")
          (str "CREATE TRIGGER update_" posts-table "_updated BEFORE "
               "UPDATE ON " posts-table " FOR EACH ROW "
               "EXECUTE PROCEDURE update_updated_column();")
          (str "CREATE TRIGGER update_" users-table "_updated BEFORE "
               "UPDATE ON " users-table " FOR EACH ROW "
               "EXECUTE PROCEDURE update_updated_column();"))
 	 nil))
   (catch Exception e (.getNextException e))))

(defn create-tables []
  (do
    (create-posts-table mydb)
    (create-users-table mydb)
    (create-functions-etc mydb)
    (mload/load-posts-table mydb)
    (mload/load-users-table mydb)))

(defn migrate-tables []
  (do
    (migrate-users-table-1 mydb)
    (migrate-users-table-2 mydb)))

(defn -main []
  (print "Creating DB structure...") (flush)
  (create-tables)
  (println " done"))

(ns multiplex.models.schema
  (:use     [korma.db :only (defdb mysql postgres)]
            [multiplex.config :only (mydb)]
            [multiplex.models.db :only (posts-table tags-table users-table)])
  (:require [clojure.java.jdbc :as sql]
            [multiplex.models.load :as load]
            [multiplex.util :as util]))

(defn create-posts-table
  [db-cred]
  (try
    (sql/with-connection db-cred
      (if (= "postgres" (subs db-cred 0 8))
        (sql/do-commands
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
        (sql/do-commands
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
    (sql/with-connection db-cred
      (if (= "postgres" (subs db-cred 0 8))
        (sql/do-commands
          (str "CREATE SEQUENCE " users-table "_id_seq"
               "    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;")
          (str "CREATE TABLE " users-table " ("
            "uid integer NOT NULL PRIMARY KEY DEFAULT nextval('" users-table "_id_seq'), "
            "username character varying(100) NOT NULL UNIQUE, "
            "email character varying(100) NOT NULL UNIQUE, "
            "password character varying(128) NOT NULL, "
            "apikey character varying(64) NOT NULL UNIQUE, "
            "signupcode character varying(64) DEFAULT NULL::character varying, "
            "created timestamp(0) without time zone DEFAULT now(), "
            "updated timestamp(0) without time zone NOT NULL)"))
        (sql/do-commands
          (str "CREATE TABLE `" users-table "` (`uid` int(10) unsigned NOT NULL AUTO_INCREMENT, "
            "`username` varchar(100) COLLATE utf8_unicode_ci NOT NULL, "
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
     (catch Exception e (.getNextException e))))

(defn create-functions-etc
  [db-cred]
  (try
    (sql/with-connection db-cred
      (if (= "postgres" (subs db-cred 0 8))
        (sql/do-commands
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
    (load/load-posts-table mydb)
    (load/load-users-table mydb)))

(defn -main []
  (print "Creating DB structure...") (flush)
  (create-tables)
  (println " done"))

(ns multiplex.models.load
  (:use     [korma.db :only (defdb mysql postgres)]
            [multiplex.config :only (mydb)]
            [multiplex.models.db :only (posts-table tags-table users-table)])
  (:require [clojure.java.jdbc :as sql]
            [multiplex.util :as util]))

(defn load-posts-table
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
         (str "INSERT INTO " posts-table " VALUES "
           "(1, 1, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')")
         (str "INSERT INTO " posts-table " VALUES "
           "(2, 2, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')")
         (str "SELECT setval('" posts-table "_id_seq', (SELECT MAX(id) FROM " posts-table "));"))
        (sql/db-do-commands conn
          (str "INSERT INTO `" posts-table "` (`id`, `author`, `itemtype`, `url`, `txt`, `meta`, `tag`, `created`, `updated`) VALUES "
            "(1, 1, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')")
          (str "INSERT INTO `" posts-table "` (`id`, `author`, `itemtype`, `url`, `txt`, `meta`, `tag`, `created`, `updated`) VALUES "
            "(2, 2, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')"))))
     (catch Exception e (.getNextException e))))

(defn load-users-table
  [db-cred]
  (try
    (sql/with-db-connection [conn db-cred]
      (if (= "postgres" (subs db-cred 0 8))
        (sql/db-do-commands conn
          (str "INSERT INTO " users-table " VALUES "
            "(1, 'test', 'example1@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac8', '2484f93a0768a4dd883ac07d0edebb47', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');")
          (str "INSERT INTO " users-table " VALUES "
            "(2, 'example', 'example2@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac9', '2484f93a0768a4dd883ac07d0edebb48', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');")
         (str "SELECT setval('" users-table "_id_seq', (SELECT MAX(uid) FROM " users-table "));"))
        (sql/db-do-commands conn
          (str "INSERT INTO `" users-table "` VALUES "
            "(1, 'test', 'example1@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac8', '2484f93a0768a4dd883ac07d0edebb47', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');")
          (str "INSERT INTO `" users-table "` VALUES "
            "(2, 'example', 'example2@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac9', '2484f93a0768a4dd883ac07d0edebb48', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');"))))
     (catch Exception e (.getNextException e))))

(defn -main []
  (print "Creating DB structure...") (flush)
  (load-posts-table mydb)
  (load-users-table mydb)
  (println " done"))

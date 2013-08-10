(ns multiplex.models.load
  (:use     [korma.db :only (defdb mysql postgres)])
  (:require [clojure.java.jdbc :as sql]
            [multiplex.config :as config]
            [multiplex.util :as util]))

(defn load-posts-table
  [db-cred]
  (try
    (sql/with-connection db-cred
      (if (= "postgres" (subs db-cred 0 8))
        (sql/do-commands
         (str "INSERT INTO clj VALUES "
           "(1, 1, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')"))
        (sql/do-commands
          (str "INSERT INTO `clj` (`id`, `author`, `itemtype`, `url`, `txt`, `meta`, `tag`, `created`, `updated`) VALUES "
           "(1, 1, 'link', 'http://torret.org', 'torret', '', 'tumblelog', '2013-08-10 16:23:42', '2013-08-10 16:23:42')"))))
     (catch Exception e (.getNextException e))))

(defn load-users-table
  [db-cred]
  (try
    (sql/with-connection db-cred
      (if (= "postgres" (subs db-cred 0 8))
        (sql/do-commands
          (str "INSERT INTO users VALUES "
            "(1, 'multiplex', 'example@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac8', '2484f93a0768a4dd883ac07d0edebb47', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');"))
        (sql/do-commands
          (str "INSERT INTO `users` VALUES "
            "(1, 'multiplex', 'example@example.org', 'efa0977fe7e2e055083b1bd7136c9a64403dee8714e9969483f3aaba07914313c74ff38d1fe44fc639df0caaba0f60affd07d7824f228c4a56198d1d4fc71ac8', '2484f93a0768a4dd883ac07d0edebb47', '', '2013-08-10 16:23:42', '2013-08-10 16:23:42');"))))
     (catch Exception e (.getNextException e))))

(defn -main []
  (print "Creating DB structure...") (flush)
  (load-posts-table config/mydb)
  (load-users-table config/mydb)
  (println " done"))

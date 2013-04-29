(ns simplex-clj.models.load
  (:use     [korma.db :only (defdb mysql)])
  (:require [clojure.java.jdbc :as sql]
            [simplex-clj.util :as util]))


(defn load-posts-table
  [db-cred]
  (sql/with-connection db-cred
    (sql/do-commands
      (str "INSERT INTO `clj` (`id`, `author`, `itemtype`, `url`, `txt`, `meta`, `tag`, `created`, `updated`) VALUES
(1, 'wink', 'link', 'http://www.luminusweb.net', 'luminus', '', 'luminus,clojure', '2013-04-09 21:48:45', '2013-04-10 22:34:54'),
(2, 'wink', 'image', '/dump/11d2b14133e689ac5712de1f3d5e583d7c4b1e34.png', 'TXT', '', 'foo', '2013-04-10 22:31:30', '2013-04-10 22:34:54'),
(7, 'wink', 'image', '/dump/151c44cbd783550d93343c63f252a6cd5e094db4.jpg', 'TXT', '', 'foo', '2013-04-10 22:39:38', '2013-04-11 23:01:06'),
(9, 'wink', 'image', '/dump/9061074f6d2c6af19631cfdd36157932aaf8489d.jpg', 'TXT', '964:1379', 'foo', '2013-04-10 22:41:24', '2013-04-10 22:41:24'),
(10, 'wink', 'link', 'http://www.liftweb.net', 'liftweb', '', 'foo', '2013-04-10 22:44:43', '2013-04-10 22:44:43'),
(11, 'wink', 'video', 'https://www.youtube.com/watch?v=1ZxcfdHJBMw', 'liftweb', '', 'foo', '2013-04-10 22:59:57', '2013-04-10 22:59:57'),
(12, 'wink', 'video', 'http://vimeo.com/60345640', 'SNL: Don''t Buy Stuff You Cannot Afford', '', 'foo', '2013-04-11 15:56:43', '2013-04-11 15:56:43'),
(13, 'wink', 'text', '', 'Live fast. Die young. Respawn. Take revenge.', '', 'foo', '2013-04-11 22:46:29', '2013-04-11 22:49:45');"))))


(defn -main []
  (print "Creating DB structure...") (flush)
  (load-posts-table (System/getenv "CLEARDB_DATABASE_URL"))
  (println " done"))

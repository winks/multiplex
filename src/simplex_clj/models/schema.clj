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

(defn create-users-table
  []
  (sql/with-connection db-spec
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

(defn create-tables
  "creates the database tables used by the application"
  []
  (create-users-table))

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

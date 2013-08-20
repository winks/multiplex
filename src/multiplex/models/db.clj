(ns multiplex.models.db
  (:use korma.core
        [korma.db :only (defdb mysql postgres)])
  (:require [multiplex.config :as config]))

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

(defdb dbm
  (let [x (convert-db-uri config/mydb)]
    (if (= "postgres" (:type x))
      (postgres (assoc x :delimiters ""))
      (mysql x))))

(defentity clj)
(defentity tags)
(defentity users)

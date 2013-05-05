(ns multiplex.models.db
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]))

(defn convert-db-uri
  "convert a JDBC/Heroku DB-DSN/URI to parts"
  [db-uri]
  (let [[_ user password host port db x y] (re-matches #"mysql://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)(?:\?(.+)=(.+))?" db-uri)]
  (println (str "Connecting to DB: " user ":" "XXX@" host "/" db " " x y))
    {
      :user user
      :password password
      :host host
      :db db
      :delimiters "`"
      (keyword x) y
    }))

(defdb dbm (mysql(convert-db-uri config/mydb)))

(defentity tags)

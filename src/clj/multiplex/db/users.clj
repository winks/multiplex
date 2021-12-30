(ns multiplex.db.users
  (:require
   [multiplex.config :as config]
   [multiplex.db.core :as db]
   [multiplex.util :as util]))

(defn urlize [coll request]
  (let [url (util/make-url (:page-scheme (config/env :multiplex)) (:hostname coll) request)]
    (assoc coll :url url)))

(defn get-user-by-hostname [params & [request]]
  (let [orig (db/get-user-by-hostname params)]
    (urlize orig request)))

(defn get-profile [params & [request]]
  (let [orig (db/get-profile params)]
    (urlize orig request)))

(defn get-public-users [request]
  (let [users (remove :is_private (db/get-all-users))]
    (map #(urlize % request) users)))
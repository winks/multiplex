(ns multiplex.db.users
  (:require
    [clojure.tools.logging :as log]
    [multiplex.config :as config]
    [multiplex.db.core :as db]
    [multiplex.util :as util]))

(def user-fields [:uid :username :email :avatar :title :theme :is_private])

(defn urlize [coll request]
  (let [url (util/make-url (:hostname coll) (config/env :multiplex))]
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

(defn update-profile! [params orig]
  (let [params (select-keys (or params {}) user-fields)
        uid (util/int-or (:uid params) 0)
        avatar (:avatar params)
        username (:username params)
        email (:email params)
        title (:title params)
        theme (:theme params)
        is_private (:is_private params)
        is_private (if (boolean? is_private) is_private false)
        crit {:uid uid :username username :email email :avatar avatar
              :title title :theme theme :is_private is_private
              :hostname (:hostname orig)}]
    (log/debug "dbp/update-profile!" crit)
    (db/update-user! crit)))
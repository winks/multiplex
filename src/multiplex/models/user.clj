(ns multiplex.models.user
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]
            [multiplex.models.db :as db]))

(defentity users)

; users
(defn create-user [user]
  (insert users
          (values user)))

(defn update-user [id first-name last-name email]
  (update users
  (set-fields {:first_name first-name
               :last_name last-name
               :email email})
  (where {:id id})))

(defn get-user-by-id
  [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))

(defn get-user-by-key
  [apikey]
  (first (select users
                 (where {:apikey apikey})
                 (limit 1))))

(defn get-user-by-name
  [username]
  (first (select users
                 (where {:username username})
                 (limit 1))))

(defn valid-apikey?
  [apikey]
  (not (nil? (get-user-by-key apikey))))

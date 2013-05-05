(ns multiplex.models.user
  (:use korma.core
        [korma.db :only (defdb mysql)])
  (:require [multiplex.config :as config]
            [multiplex.models.db :as db]
            [multiplex.util :as util]))

; prepare 
(defn prepare-map []
  {:uid nil
   :username nil
   :email nil
   :password nil
   :apikey nil
   :signupcode nil
   :created nil
   :updated nil})

; SQLish

(defn new-user [user]
  (insert db/users
          (values user)))

(defn update-user-credentials
  "Update password and apikey for a user."
  [uid password apikey]
  (update db/users
    (set-fields {:password password
                 :apikey apikey})
    (where {:uid uid})))

(defn get-user-by-id
  [id]
  (first (select db/users
          (where {:id id})
          (limit 1))))

(defn get-user-by-key
  [apikey]
  (first (select db/users
           (where {:apikey apikey})
           (limit 1))))

(defn get-user-by-name
  [username]
  (first (select db/users
           (where {:username username})
           (limit 1))))

; abstraction

(defn valid-apikey?
  "Whether a given apikey is valid."
  [apikey]
  (not (nil? (get-user-by-key apikey))))

(defn create-user [params]
  (if (seq (:username params))
    (let [db-params  (merge (prepare-map) (assoc params :apikey "FIXME" :password "FIXME"))
          uid        (:GENERATED_KEY (new-user db-params))
          new-apikey (util/hash-apikey (config/salt-apikey (:username params) (:password params) uid))
          new-pass   (util/hash-password (config/salt-password (:username params) (:password params) uid))]
      (do
        (update-user-credentials uid new-pass new-apikey)
        {:username (:username params) :apikey new-apikey :content true}))
    false))

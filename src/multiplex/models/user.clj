(ns multiplex.models.user
  (:require [korma.core :refer :all]
            [clojure.java.jdbc :as sql]
            [multiplex.config :as config]
            [multiplex.models.db :as db]
            [multiplex.util :as util]))

; prepare
(defn prepare-map []
  {:uid nil
   :username nil
   :hostname nil
   :email nil
   :password nil
   :apikey nil
   :signupcode nil
   :created nil
   :updated nil
   :title nil
   :theme nil
   })

; SQLish

(defn new-user [user]
  (let [query (str "INSERT INTO " db/users-table
                "(uid, username, hostname, email, password, apikey, signupcode, created, updated, title, theme)"
                " VALUES(nextval('" db/users-table "_id_seq'), ?, ?, ?, ?, ?, ?, NOW(), NOW(), '', '') RETURNING uid;")]
    (sql/query config/mydb [query (:username user) (:hostname user) (:email user) (:password user) (:apikey user) (:signupcode user)])))


(defn update-user-credentials
  "Update password and apikey for a user."
  [uid password apikey]
  (update db/mpx_users
    (set-fields {:password password
                 :apikey apikey})
    (where {:uid uid})))

(defn get-user-by-id
  [id]
  (first (select db/mpx_users
          (where {:id id})
          (limit 1))))

(defn get-user-by-key
  [apikey]
  (first (select db/mpx_users
           (where {:apikey apikey})
           (limit 1))))

(defn get-user-by-name
  [username]
  (first (select db/mpx_users
           (where {:username username})
           (limit 1))))

(defn get-user-by-hostname
  [s]
  (first (select db/mpx_users
          (where {:hostname s})
          (limit 1))))

(defn get-public-users
  []
  (select db/mpx_users
   (fields :uid :username :hostname :title :avatar)
    (where {:private false})
    (order :hostname :ASC)))

; abstraction

(defn valid-apikey?
  "Whether a given apikey is valid."
  [apikey]
  (not (nil? (get-user-by-key apikey))))

(defn create-user [params]
  (if (seq (:username params))
    (let [db-params  (merge (prepare-map) (assoc params :apikey "FIXME" :password "FIXME" :hostname "FIXME"))
          uid        (:GENERATED_KEY (new-user db-params))
          new-apikey (util/hash-apikey (config/salt-apikey (:username params) (:password params) uid))
          new-pass   (util/hash-password (config/salt-password (:username params) (:password params) uid))]
      (do
        (update-user-credentials uid new-pass new-apikey)
        {:username (:username params) :apikey new-apikey :content true}))
    false))

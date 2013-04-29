(ns simplex-clj.models.db
  (:use korma.core
        [korma.db :only (defdb mysql)]))

(def itemtypes '("image" "link" "text" "video"))

(defn convert-db-uri [db-uri]
  (let [[_ user password host port db x y] (re-matches #"mysql://(?:(.+):(.*)@)?([^:]+)(?::(\d+))?/(.+)(?:\?(.+)=(.+))" db-uri)]
    {
      :user user
      :password password
      :host host
      :port (or port 80)
      :db db
      (keyword x) y
    }))

(defdb dbm
  (mysql
    (convert-db-uri
      (System/getenv "CLEARDB_DATABASE_URL"))))

(defentity users)
(defentity clj)

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

(defn get-user [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))
; posts
(defn get-post [id]
  (do
    (println (sql-only (select clj (where {:id id}))))
    (select clj (where {:id id}))))

(defn get-posts
  ([n]
    (get-posts n 0))
  ([n off]
    (do
      (println (sql-only
        (select clj
                (order :id :DESC)
                (limit n)
                (offset off))))
      (select clj
            (order :id :DESC)
            (limit n)
            (offset off)))))

(defn new-post [params]
  (insert clj
    (values params)))

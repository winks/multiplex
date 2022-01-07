(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [buddy.hashers :as hashers]
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [expound.alpha :as expound]
    [mount.core :as mount]
    [multiplex.config :refer [env]]
    [multiplex.core :refer [start-app]]
    [multiplex.db.core]
    [multiplex.util :as util]
    [conman.core :as conman]
    [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'multiplex.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'multiplex.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn restart-db
  "Restarts database."
  []
  (mount/stop #'multiplex.db.core/*db*)
  (mount/start #'multiplex.db.core/*db*)
  (binding [*ns* (the-ns 'multiplex.db.core)]
    (conman/bind-connection multiplex.db.core/*db* "sql/queries.sql")))

(defn reset-db
  "Resets database."
  []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate
  "Migrates database up for all outstanding migrations."
  []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback
  "Rollback latest database migration."
  []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration
  "Create a new up and down migration file with a generated timestamp and `name`."
  [name]
  (migrations/create name (select-keys env [:database-url])))

(defn create-user
  "Create a new user via multiplex.db.core/create-user!, optional: apikey signupcode theme avatar is_active is_private"
  [params]
  (let [apikey     (or (:apikey params) "")
        signupcode (or (:signupcode params) "")
        theme      (or (:theme params) "default")
        avatar     (or (:avatar params) "/img/map2/default-avatar.png")
        is_active  (or (:is_active params) true)
        is_private (or (:is_private params) false)
        password   (hashers/derive (:password params))
        p2 (assoc params :apikey apikey
                         :signupcode signupcode
                         :theme theme
                         :password password
                         :is_active is_active
                         :is_private is_private)]
    (multiplex.db.core/create-user! p2)))

(defn create-post
  "Create a new post via multiplex.db.core/create-post!, optional: itemtype url txt meta tag"
  [params]
  (let [itemtype (or (:itemtype params) "link")
        url      (or (:url params) "")
        txt      (or (:txt params) "")
        meta     (or (:meta params) "{}")
        tag      (or (:tag params) "")
        p2 (assoc params :itemtype itemtype
                         :url url
                         :txt txt
                         :meta meta
                         :tag tag)]
    (multiplex.db.core/create-post! p2)))

(defn change-password!
  "Change a user's password, {:uid :password}"
  [params]
  (let [uid (util/int-or (:uid params) 0)]
    (cond
      (empty? (:password params)) "Password too short"
      (< (count (str (:password params))) 4) "Password too short"
      (not (pos? uid)) (str "Wrong uid: " (:uid params))
      :else (multiplex.db.core/change-password! (assoc params :uid uid)))))

(defn list-users
  []
  (println (str (format "%3s"  "uid")      " | "
                (format "%20s" "username") " | "
                (format "%20s" "hostname") " | "
                (format "%10s" "theme")    " | "
                (format "%10s" "active?")  " | "
                (format "%10s" "private?")))
  (println (apply str (repeat 80 "-")))
  (let [users (multiplex.db.core/get-all-users-internal)]
    (for [usr users]
      (println (str (format "%3d"  (:uid usr))       " | "
                    (format "%20s" (:username usr))  " | "
                    (format "%20s" (:hostname usr))  " | "
                    (format "%10s" (:theme usr))     " | "
                    (format "%10s" (:is_active usr)) " | "
                    (format "%10s" (:is_private usr)
                    ))))))
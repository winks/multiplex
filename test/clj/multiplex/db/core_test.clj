(ns multiplex.db.core-test
  (:require
    [clojure.test :refer :all]
    [java-time.pre-java8]
    [luminus-migrations.core :as migrations]
    [mount.core :as mount]
    [multiplex.config :refer [env]]
    [multiplex.db.core :refer [*db*] :as db]
    [multiplex.layout :as layout]
    [next.jdbc :as jdbc]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'multiplex.config/env
     #'multiplex.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-get-login
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [rv (db/create-user!
              t-conn
              {:username "testman"
               :hostname "u123.example.org"
               :email    "testman@example.org"
               :password "my_password"
               :title    "u123_title"
               :avatar   "/tmp/avatar.jpg"
               :theme    "my_theme"
               :apikey   "testapikey"
               :signupcode   "testsignupcode"
               :is_active true
               :is_private false
               }
              {})
          uid (:uid (first rv))]

    (is (= {:uid      uid
            :username "testman"
            :password "my_password"}
           (db/get-login t-conn {:username "testman"} {})))
           (.rollback t-conn))))

(deftest test-layout-prepare-params-author
  ;(jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [rv (db/create-user!
              ;t-conn
              {:username "testman"
               :hostname "u123.example.org"
               :email    "testman@example.org"
               :password "my_password"
               :title    "u123_title"
               :avatar   "/tmp/avatar.jpg"
               :theme    "my_theme"
               :apikey   "testapikey"
               :signupcode   "testsignupcode"
               :is_active true
               :is_private false
               })
              ;{})
          uid (:uid (first rv))
          req {:scheme :https
               :server-name "u123.example.org"
               :server-port 2000}]

    (is (= {:username "testman",
            :hostname "u123.example.org",
            :title "u123_title",
            :avatar "/tmp/avatar.jpg",
            :theme "my_theme",
            :is_private false,
            :uid uid,
            :url "http://u123.example.org:3030"} (layout/prepare-params-author req {})))
    (db/delete-user! {:uid uid}) ))
           ;(.rollback t-conn))))

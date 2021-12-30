(ns multiplex.db.core-test
  (:require
   [multiplex.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [multiplex.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'multiplex.config/env
     #'multiplex.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(comment

(deftest test-users
  (db/delete-user! {:uid 1})
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= [{:uid 1}] (db/create-user!
              t-conn
              {:username "testman"
               :hostname "test.example.org"
               :email    "testman@example.org"
               :password "my_password"
               :title    "My Title"
               :avatar   "/tmp/avatar.jpg"
               :theme    "my_theme"
               }
              {})))
    (is (= {:uid      1
            :username "testman"
            :password "my_password"}
           (db/get-login t-conn {:username "testman"} {})))
           (.rollback t-conn)))

)
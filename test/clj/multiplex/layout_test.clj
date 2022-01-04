(ns multiplex.layout-test
  (:require
    [clojure.test :refer :all]
    [mount.core :as mount]
    [multiplex.config :as config]
    [multiplex.db.core :refer [*db*] :as db]
    [multiplex.layout :as layout]
    [multiplex.util :refer :all]
    [next.jdbc :as jdbc]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'multiplex.config/env
                 #'multiplex.db.core/*db*)
                 ;#'multiplex.handler/app-routes)
    (f)))

(deftest test-get-version-0
  (let [prop nil]
    (is (= "" (layout/get-version prop)))))

(deftest test-get-version-01
  (let [prop (java.util.Properties.)]
    (is (= "" (layout/get-version prop)))))

(deftest test-get-version-1
  (let [prop (doto (java.util.Properties.)
             (.setProperty "version" "1.0")
             (.setProperty "revision" "abcdef0"))]
    (is (= "1.0" (layout/get-version prop)))))

(deftest test-get-version-2
  (let [prop (doto (java.util.Properties.)
             (.setProperty "version" "1.0-SNAPSHOT")
             (.setProperty "revision" "abcdef01"))]
    (is (= "1.0-dev abcdef0" (layout/get-version prop)))))

(deftest test-get-version-3
  (let [prop (doto (java.util.Properties.)
             (.setProperty "version" "1.0-foo")
             (.setProperty "revision" "abcdef0"))]
    (is (= "1.0-foo" (layout/get-version prop)))))

(deftest test-prepare-1
  (let [session {:user "testuser" :uid 23}
        request {:scheme :https
                 :server-name "sub.example.org"
                 :server-port 2000
                 :flash "FlashMessage"
                 :session session}
        params  {:limit 11 :page 3 :type "video"
                 :pcount 666 :modus :foo
                 :post {:author {:uid "42" :theme "default2" :title "MyTitle"}}}
        p2 (layout/prepare-params request params)]
    ; pagination
    (is (= 61 (count (:pages (:pagi p2)))))
    (is (= 1 (first (:pages (:pagi p2)))))
    (is (= 61 (last (:pages (:pagi p2)))))
    (is (= {:page-newer 2
            :page-older 4
            :pages [1 2]
            :page-count 61
            :post-count 666
            :limit 11
            :page 3
            :type "video"} (assoc (:pagi p2) :pages [1 2])))
    ; navigation
    (is (= {:type-link "?type=link&limit=11&page=3"
            :type-text "?type=text&limit=11&page=3"
            :type-image "?type=image&limit=11&page=3"
            :type-audio "?type=audio&limit=11&page=3"
            :type-video "?type=video&limit=11&page=3"} (:navi p2)))
    ; auth
    (is (= {:loggedin true
            :user "testuser"
            :uid 23} (:auth p2)))
    ; glob
    (is (= "MyTitle" (:page-header (:glob p2))))))

(deftest test-prepare-glob-0-1 ; empty title | username |theme | uid | modus | flash
  (binding [layout/pom-config (doto (java.util.Properties.)
                                    (.setProperty "version" "foo"))]
    (let [session {}
          request {:scheme :https
                   :server-name "sub.example.org"
                   :server-port 2000
                   :session session}
          params  {:limit 11 :page 3 :type "video"
                   :pcount 666
                   :post {:author {:uid 0}}}
          glob (layout/prepare-params-glob request params)]
      (is (= "multiplex-testing" (:page-header glob)))
      (is (= "multiplex-testing" (:site-title glob)))
      (is (= "default-test" (:theme glob)))
      (is (= "0" (:favicon glob)))
      (is (= "" (:modus glob)))
      (is (= "http://static.mpx1.f5n.de" (:assets-prefix glob)))
      (is (= nil (:flash glob)))
      (is (= "foo" (:version-string glob))))))

(deftest test-prepare-glob-0-2 ; empty title
  (binding [layout/pom-config (doto (java.util.Properties.)
                                    (.setProperty "version" "foo"))]
    (let [session {}
          request {:scheme :https
                   :server-name "sub.example.org"
                   :server-port 2000
                   :session session}
          params  {:limit 11 :page 3 :type "video"
                   :pcount 666
                   :post {:author {:uid 23 :username "MyUser"}}}
          glob (layout/prepare-params-glob request params)]
      (is (= "MyUser's multiplex" (:page-header glob)))
      (is (= "23" (:favicon glob)))
      (is (= "foo" (:version-string glob))))))

(comment

(deftest test-prepare-glob-author-1
  (binding [layout/pom-config (doto (java.util.Properties.)
                                    (.setProperty "version" "foo"))]
;            db/get-user-by-hostname2
;      (fn [crit] {:uid 123 :username "u123" :hostname "u123.example.org"
;                  :title "u123_title" :avatar "u123.png"
;                  :theme "u123-theme" :is_private false})]
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [session {:user "testuser" :uid 23}
          request {:scheme :https
                   :server-name "u123.example.org"
                   :server-port 2000
                   :flash "FlashMessage"
                   :session session}
          params  {:limit 11 :page 3 :type "video"
                   :pcount 666 :modus :foo
                   :post {}}
          dbrv
			  xx (println dbrv)
			  xx (println (multiplex.db.core/get-user {:uid (:uid (first dbrv))}))
          glob (layout/prepare-params-glob request params)]
    (is (= {:page-header "u123_title"
            :site-title "u123_title"
            :theme "my_theme"
            :favicon "42"
            :modus "foo"
            :assets-prefix "http://static.mpx1.f5n.de"
            :base-url "http://mpxtest.f5n.de:3030"
            :version-string "42.42-dev 0fedcba"
            :flash "FlashMessage"} glob))
    (.rollback t-conn)))))

)
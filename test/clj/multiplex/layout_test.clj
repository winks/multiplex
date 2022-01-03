(ns multiplex.layout-test
  (:require
    [clojure.test :refer :all]
    [mount.core :as mount]
    [multiplex.config :as config]
    [multiplex.layout :as layout]
    [multiplex.util :refer :all]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'multiplex.config/env)
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
    (is (= {:page-header "MyTitle"
            :site-title "MyTitle"
            :theme "default2"
            :favicon "42"
            :modus "foo"
            :assets-prefix "http://static.mpx1.f5n.de"
            :base-url "http://mpxtest.f5n.de:3030"
            :version-string "2.0.2-dev 6c4e6e5"
            :flash "FlashMessage"} (:glob p2)))
    ))
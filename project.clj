(defproject
  multiplex
  "0.2.6-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.5.1"]
   [lib-noir "0.4.9"]
   [compojure "1.1.5"]
   [ring-server "0.2.7"]
   [clabango "0.5"]
   [com.taoensso/timbre "1.5.2"]
   [com.taoensso/tower "1.4.0"]
   [markdown-clj "0.9.19"]
   [org.clojure/java.jdbc "0.2.3"]
   [mysql/mysql-connector-java "5.1.6"]
   [korma "0.3.0-RC5"]
   [digest "1.3.0"]
   [org.clojure/data.json "0.2.2"]
   [org.clojure/tools.trace "0.7.5"]
   [log4j
    "1.2.15"
    :exclusions
    [javax.mail/mail
     javax.jms/jms
     com.sun.jdmk/jmxtools
     com.sun.jmx/jmxri]]]
  :ring
  {:handler multiplex.handler/war-handler,
   :init multiplex.handler/init,
   :destroy multiplex.handler/destroy}
  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}},
   :dev
   {:dependencies [[ring-mock "0.1.3"] [ring/ring-devel "1.1.8"]]}}
  :url
  "https://github.com/winks/multiplex"
  :plugins
  [[lein-ring "0.8.3"]]
  :description
  "A little tumblelog"
  :min-lein-version "2.0.0")
(comment [com.h2database/h2 "1.3.170"])

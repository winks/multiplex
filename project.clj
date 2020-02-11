(defproject multiplex "0.3.4-dev"
  :description "A little tumblelog"
  :url "https://github.com/winks/multiplex"
  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/data.json "0.2.6"]
   [org.clojure/java.jdbc "0.3.7"]
   [org.clojure/tools.trace "0.7.9"]
   [clj-rss "0.2.3"]
   [clj-time "0.14.4"]
   [compojure "1.6.1"]
   [digest "1.4.8"]
   [korma "0.3.3"]
   [lib-noir "0.9.9"]
   [markdown-clj "1.0.2"]
   [ring-server "0.5.0"]
   [selmer "1.11.7"]
   [com.taoensso/timbre "4.10.0"]
   [com.taoensso/tower "3.1.0-beta5" :exclusions [org.clojure/clojure]]
   [mysql/mysql-connector-java "5.1.31"]
   [org.postgresql/postgresql "42.2.2.jre7"]
   [log4j "1.2.17" :exclusions [javax.mail/mail
                                javax.jms/jms
                                com.sun.jdmk/jmxtools
                                com.sun.jmx/jmxri]]]
  :plugins [[lein-ring "0.12.5"]
            [lein-kibit "0.1.8"]]
  :ring {:handler multiplex.handler/app,
         :init    multiplex.handler/init,
         :destroy multiplex.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring/ring-mock "0.3.2"]
                        [ring/ring-core "1.6.3"]
                        [pjstadig/humane-test-output "0.8.3"]]
         :env {:dev true}
         :ring {:open-browser? false}}}
  :min-lein-version "2.7.0")

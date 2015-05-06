(defproject multiplex "0.3.1-dev"
  :description "A little tumblelog"
  :url "https://github.com/winks/multiplex"
  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [lib-noir "0.8.4"]
   [compojure "1.1.8"]
   [ring-server "0.3.1"]
   [selmer "0.6.9"]
   [clj-time "0.8.0"]
   [com.taoensso/timbre "3.2.1"]
   [com.taoensso/tower "2.0.2"]
   [markdown-clj "0.9.47"]
   [org.clojure/java.jdbc "0.3.6"]
   [mysql/mysql-connector-java "5.1.31"]
   [postgresql "9.3-1101.jdbc4"]
   [korma "0.3.3"]
   [digest "1.4.4"]
   [org.clojure/data.json "0.2.5"]
   [org.clojure/tools.trace "0.7.8"]
   [log4j "1.2.15" :exclusions [javax.mail/mail
                                javax.jms/jms
                                com.sun.jdmk/jmxtools
                                com.sun.jmx/jmxri]]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler multiplex.handler/app,
         :init    multiplex.handler/init,
         :destroy multiplex.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.3.0"]
                        [pjstadig/humane-test-output "0.6.0"]]
         :env {:dev true}
         :ring {:open-browser? false}}}
  :min-lein-version "2.0.0")

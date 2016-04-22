(defproject multiplex "0.3.1-dev"
  :description "A little tumblelog"
  :url "https://github.com/winks/multiplex"
  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [lib-noir "0.9.9"]
   [compojure "1.4.0"]
   [ring-server "0.4.0"]
   [selmer "0.9.2"]
   [clj-time "0.11.0"]
   [com.taoensso/timbre "4.1.2"]
   [com.taoensso/tower "3.0.0" :exclusions [org.clojure/clojure]]
   [markdown-clj "0.9.47"]
   [org.clojure/java.jdbc "0.3.7"]
   [mysql/mysql-connector-java "5.1.31"]
   [postgresql "9.3-1102.jdbc41"]
   [korma "0.3.3"]
   [digest "1.4.4"]
   [org.clojure/data.json "0.2.6"]
   [org.clojure/tools.trace "0.7.8"]
   [log4j "1.2.15" :exclusions [javax.mail/mail
                                javax.jms/jms
                                com.sun.jdmk/jmxtools
                                com.sun.jmx/jmxri]]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler multiplex.handler/app,
         :init    multiplex.handler/init,
         :destroy multiplex.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring/ring-mock "0.3.0"]
                        [ring/ring-core "1.4.0"]
                        [pjstadig/humane-test-output "0.7.0"]]
         :env {:dev true}
         :ring {:open-browser? false}}}
  :min-lein-version "2.0.0")

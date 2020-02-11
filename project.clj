(defproject multiplex "0.3.5"
  :description "A little tumblelog"
  :url "https://github.com/winks/multiplex"
  :dependencies
  [[org.clojure/clojure "1.10.1"]
   [org.clojure/data.json "0.2.7"]
   [org.clojure/java.jdbc "0.5.8"]
   [clj-rss "0.2.5"]
   [clj-time "0.15.2"]
   [compojure "1.6.1"]
   [digest "1.4.9"]
   [korma "0.3.3"]
   [lib-noir "0.9.9"]
   [markdown-clj "1.10.1"]
   [ring-server "0.5.0"]
   [selmer "1.12.18"]
   [mysql/mysql-connector-java "5.1.48"]
   [org.postgresql/postgresql "42.2.10.jre7"]]
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
   :dev {:dependencies [[ring/ring-core "1.8.0"]]
         :env {:dev true}
         :ring {:open-browser? false}}}
  :min-lein-version "2.7.0")

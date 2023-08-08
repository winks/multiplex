(defproject multiplex "2.0.7"

  :description "multiplex - a small tumblelog"
  :url "http://github.com/winks/multiplex"

  :dependencies [[buddy/buddy-auth "3.0.323"]
                 [buddy/buddy-core "1.11.423"]
                 [buddy/buddy-hashers "2.0.167"]
                 [buddy/buddy-sign "3.5.351"]
                 [ch.qos.logback/logback-classic "1.4.9"]
                 [clojure.java-time "1.2.0"]
                 [conman "0.9.6"]
                 [cprop "0.1.19"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.6"]
                 [luminus-undertow "0.1.18"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.11.4"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.18"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.17"]
                 [nrepl "1.0.0"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.219"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.postgresql/postgresql "42.6.0"]
                 [org.webjars.npm/bulma "0.9.4"]
                 [org.webjars.npm/material-icons "1.13.2"]
                 [org.webjars/webjars-locator "0.47"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-ssl "0.3.0"]
                 [selmer "1.12.55"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot multiplex.core

  :plugins [[lein-kibit "0.1.8"]
            [lein-cloverage "1.2.2"]]

  :profiles
  {:uberjar {:omit-source true
             :aot :all
             :uberjar-name "multiplex.jar"
             :source-paths ["env/prod/clj" ]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[org.clojure/tools.namespace "1.1.1"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [ring/ring-devel "1.9.4"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "1.0.0"]
                                 [cider/cider-nrepl "0.26.0"]]

                  :source-paths ["env/dev/clj" ]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"] }
   :profiles/dev {}
   :profiles/test {}})

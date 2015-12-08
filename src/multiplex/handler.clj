(ns multiplex.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [noir.util.middleware :as middleware]
            [multiplex.routes.home :refer :all]))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (println "multiplex started successfully..."))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (println "shutting down..."))

;;append your application routes to the all-routes vector
(def all-routes [home-routes app-routes])

(def app (middleware/app-handler all-routes))

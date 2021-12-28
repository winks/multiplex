(ns multiplex.routes.home
  (:require
   [multiplex.layout :as layout]
   [multiplex.db.core :as db]
   [clojure.java.io :as io]
   [multiplex.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])


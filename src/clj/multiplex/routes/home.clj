(ns multiplex.routes.home
  (:require
   [multiplex.layout :as layout]
   [multiplex.db.core :as db]
   [clojure.java.io :as io]
   [multiplex.middleware :as middleware]
   [ring.util.response :refer [response redirect]]
   [ring.util.http-response :as hresponse]))

(defn set-user! [id {session :session}]
(println "set-user!" id session)
  (-> (redirect "/")
      (assoc :session (assoc session :user id))))

(defn clear-session! [{session :session}]
  (-> (redirect "/")
      (assoc :session nil)
      (dissoc :cookies)))

(defn home-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        data {:docs (-> "docs/docs.md" io/resource slurp)
              :loggedin loggedin
              :user user}]
    (layout/render request "home.html" data)))

(defn about-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        data {:loggedin loggedin
              :user user}]
    (layout/render request "about.html" data)))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/login/:id" {:get (fn [{:keys [path-params] :as req}]
                         (set-user! (:id path-params) req))}]
   ["/logout" {:get clear-session!}]
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])


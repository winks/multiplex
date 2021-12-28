(ns multiplex.routes.home
  (:require
   [multiplex.layout :as layout]
   [multiplex.db.core :as db]
   [multiplex.db.posts :as dbp]
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

(defn posts-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        author (db/get-user-by-hostname {:hostname "t.mpx1.f5n.de"})
        xx (println author)
        posts (dbp/get-posts author)
        data {:posts posts
              :loggedin loggedin
              :user user}]
    (layout/render request "posts.html" data)))

(defn docs-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        data {:docs (-> "docs/docs.md" io/resource slurp)
              :loggedin loggedin
              :user user}]
    (layout/render request "docs.html" data)))

(defn about-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        data {:loggedin loggedin
              :user user}]
    (layout/render request "about.html" data)))

(defn home-page [request]
  (let [user (:user (:session request))
        loggedin (some? user)
        data {:posts []
              :loggedin loggedin
              :user user}]
    (layout/render request "home.html" data)))

(defn home-routes []
  [ "" 
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/login/:id" {:get (fn [{:keys [path-params] :as req}]
                         (set-user! (:id path-params) req))}]
   ["/logout" {:get clear-session!}]
   ["/posts" {:get posts-page}]
   ["/docs" {:get docs-page}]
   ["/about" {:get about-page}]
   ["/" {:get home-page}]])


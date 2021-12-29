(ns multiplex.routes.home
  (:require
   [multiplex.db.core :as db]
   [multiplex.db.posts :as dbp]
   [multiplex.db.users :as dbu]
   [multiplex.layout :as layout]
   [multiplex.middleware :as middleware]
   [multiplex.util :as util]
   [buddy.hashers :as hashers]
   [clojure.java.io :as io]
   [ring.util.response :refer [response redirect]]
   [ring.util.http-response :as hresponse]))

(defn set-login-flash! [msg {flash :flash}]
  (println "set-login-flash!" msg flash)
  (-> (redirect "/login")
      (assoc :flash msg)))

(defn set-user! [id {session :session}]
  (println "set-user!" id session)
  (-> (redirect "/")
      (assoc :session (assoc session :user id))))

(defn clear-session! [{session :session}]
  (-> (redirect "/")
      (assoc :session nil)
      (dissoc :cookies)))

(defn login! [request]
  ;(println "PW" (hashers/derive "multiplex"))
  (let [un (get (:form-params request) "username")
        pw (get (:form-params request) "password")
        dbuser (db/get-login {:username pw})
        checked (hashers/check un (:password dbuser))]
    (cond
      (empty? dbuser)              (set-login-flash! "db0" request)
      (empty? (:username dbuser))  (set-login-flash! "db1" request)
      (empty? (:password dbuser))  (set-login-flash! "db2" request)
      (not= un (:username dbuser)) (set-login-flash! "db3" request)
      (true? checked)              (set-user! un request)
      :else                        (set-login-flash! "ee" request))))

; simple pages
(defn render-page
  ([request page-name]              (layout/render2 request (str "page_" (name page-name) ".html")))
  ([request page-name data]         (layout/render2 request (str "page_" (name page-name) ".html") data))
  ([request page-name content html] (layout/render2 request (str "page_" (name page-name) ".html") {:content content :html html})))

(defn render-page-404
  ([request] (render-page-404 request "The page could not be found."))
  ([request content] (layout/error-page {:status 404 :headers {"Content-Type" "text/html"} :message content})))

(defn posts-page [request]
  (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
        mreq (select-keys request [:server-port :scheme])
        author (dbu/get-user-by-hostname {:hostname (:server-name request)} mreq)
        posts (dbp/get-posts (assoc qp :author (:uid author)) mreq)]
    (render-page request :posts {:posts posts :site author})))

(defn all-posts-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (render-page-404 request)
    (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
          mreq (select-keys request [:server-port :scheme])
          posts (dbp/get-all-posts qp mreq)]
      (render-page request :posts {:posts posts}))))

(defn about-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (render-page-404 request)
    (render-page request :about)))

(defn home-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (posts-page request)
    (render-page request :index)))

(defn login-page [request]
  (render-page request :login))

(defn users-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (render-page-404 request)
    (let [mreq (select-keys request [:server-port :scheme])
          users (dbu/get-public-users request)]
      (render-page request :users {:users users}))))

(defn home-routes []
  [ ""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/about"    {:get about-page}]
   ["/everyone" {:get all-posts-page}]
   ["/login"    {:get login-page
                 :post login!}]
   ["/logout"   {:get clear-session!}]
   ["/post/:id" {:get (fn [{:keys [path-params query-params] :as req}] (posts-page req))}]
   ["/posts"    {:get posts-page}]
   ["/users"    {:get users-page}]
   ["/"         {:get home-page}]])

;    ["/login/:id" {:get (fn [{:keys [path-params] :as req}]
;                         (set-user! (:id path-params) req))}]
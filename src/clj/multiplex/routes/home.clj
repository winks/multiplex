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

(defn logged-in? [request]
  (let [uid (or (:uid (:session request)) 0)]
    (> uid 0)))

(defn set-login-flash! [msg {flash :flash}]
  (println "set-login-flash!" msg flash)
  (-> (redirect "/login")
      (assoc :flash msg)))

(defn set-user! [name uid {session :session}]
  (println "set-user!" name uid session)
  (-> (redirect "/")
      (assoc :session (assoc session :user name :uid uid))))

(defn clear-session! [{session :session}]
  (-> (redirect "/")
      (assoc :session nil)
      (dissoc :cookies)))

(defn login! [request]
  (let [un (get (:form-params request) "username")
        pw (get (:form-params request) "password")
        dbuser (db/get-login {:username un})
        checked (hashers/check pw (:password dbuser))]
    (cond
      (empty? dbuser)              (set-login-flash! "db0" request)
      (empty? (:username dbuser))  (set-login-flash! "db1" request)
      (empty? (:password dbuser))  (set-login-flash! "db2" request)
      (not= un (:username dbuser)) (set-login-flash! "db3" request)
      (true? checked)              (set-user! (:username dbuser) (:uid dbuser) request)
      :else                        (set-login-flash! "ee" request))))

(defn add-item! [request]
  (if (not (logged-in? request))
    (redirect "/")
    (let [url (get (:form-params request) "url")
          txt (get (:form-params request) "txt")
          tags (get (:form-params request) "tags")
          result (dbp/create-post! {:url url :txt txt :tag (or tags "") :author (:uid (:session request))})]
      (-> (redirect "/add")
          (assoc :flash (:id (first result)))))))

; simple pages
(defn render-page
  ([request page-name]              (layout/render2 request (str "page_" (name page-name) ".html")))
  ([request page-name data]         (layout/render2 request (str "page_" (name page-name) ".html") data)))
  ;([request page-name content html] (layout/render2 request (str "page_" (name page-name) ".html") {:content content :html html})))

(defn render-page-404
  ([request] (render-page-404 request "The page could not be found."))
  ([request content] (layout/error-page {:status 404 :headers {"Content-Type" "text/html"} :message content})))

(defn posts-page [request]
  (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
        mreq (select-keys request [:server-port :scheme])
        author (dbu/get-user-by-hostname {:hostname (:server-name request)} mreq)
        posts (dbp/get-posts :some (assoc qp :author (:uid author)) mreq)
        author2 (util/set-author author request)]
    (render-page request :posts (merge qp {:posts (second posts) :pcount (first posts) :post {:author (:author author2)}})))) ;:subsite author2

(defn all-posts-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (render-page-404 request)
    (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
          mreq (select-keys request [:server-port :scheme])
          posts (dbp/get-posts :all qp mreq)]
      (render-page request :posts (merge qp {:posts (second posts) :pcount (first posts)})))))

(defn about-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (render-page-404 request)
    (render-page request :about)))

(defn add-page [request]
  (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
        qp (if (and (empty? (:txt qp)) (seq (:title qp))) (assoc qp :txt (util/sanitize-title (:title qp))) qp)]
  (println "add" qp)
    (render-page request :add {:form qp})))

(defn home-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (posts-page request)
    (render-page request :index)))

(defn login-page [request]
  (render-page request :login))

(defn meta-page [request]
  (let [uid (or (:uid (:session request)) 0)
        profile (if (> uid 0) (dbu/get-profile {:uid uid} request) nil)]
  (render-page request :meta {:profile profile})))

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
   ["/add"      {:get add-page
                 :post add-item!}]
   ["/everyone" {:get all-posts-page}]
   ["/login"    {:get login-page
                 :post login!}]
   ["/logout"   {:get clear-session!}]
   ["/meta"     {:get meta-page}]
   ["/post/:id" {:get (fn [{:keys [path-params query-params] :as req}] (posts-page req))}]
   ["/posts"    {:get posts-page}]
   ["/users"    {:get users-page}]
   ["/"         {:get home-page}]])

;    ["/login/:id" {:get (fn [{:keys [path-params] :as req}]
;                         (set-user! (:id path-params) req))}]
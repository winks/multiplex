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
    (pos? uid)))

(defn set-login-flash! [msg {flash :flash}]
  (println "set-login-flash!" msg flash)
  (-> (redirect "/login")
      (assoc :flash msg)))

(defn set-user! [name uid {session :session}]
  (println "set-user!" name uid session)
  (let [new-url (or (:return session) "/")
        new-session (-> session (dissoc :return) (assoc :user name) (assoc :uid uid))]
    (println "set-user!" new-url new-session)
    (-> (redirect new-url)
        (assoc :session new-session))))

(defn clear-session! [{session :session}]
  (-> (redirect "/")
      (assoc :session nil)
      (dissoc :cookies)))

(defn login! [request]
  (let [un (get (:form-params request) "username")
        pw (get (:form-params request) "password")
        dbuser (db/get-login {:username un})
        checked (hashers/check pw (:password dbuser))]
        (println "login!")
    (cond
      (empty? dbuser)              (set-login-flash! "db0" request)
      (empty? (:username dbuser))  (set-login-flash! "db1" request)
      (empty? (:password dbuser))  (set-login-flash! "db2" request)
      (not= un (:username dbuser)) (set-login-flash! "db3" request)
      (true? checked)              (set-user! (:username dbuser) (:uid dbuser) request)
      :else                        (set-login-flash! "ee" request))))

(defn add-item! [request]
  (if-not (logged-in? request)
    (redirect "/")
    (let [url (get (:form-params request) "url")
          txt (get (:form-params request) "txt")
          tags (get (:form-params request) "tags")
          editor (:uid (:session request))
          result (dbp/create-post! {:url url :txt txt :tag (or tags "") :author editor})]
          (println "add-item!" result (:session request))
      (-> (redirect "/add")
          (assoc :session (dissoc (:session request) :return))
          (assoc :flash (:id (first result)))))))

(defn delete-item! [request]
  (let [id (:id (:path-params request))
        my-url "/"]
    (if-not (logged-in? request)
      (redirect my-url)
       (let [editor (:uid (:session request))
            mreq (select-keys request [:server-port :scheme])
            posts (dbp/get-posts :some {:id id :author editor} mreq)]
        (if-not (pos? (first posts))
          (do
            (println "Failed deleting [" id "], post not found")
            (redirect my-url))
          (let [result (dbp/delete-post! {:id id})]
            (-> (redirect my-url)
                (assoc :flash id))))))))

(defn edit-item! [request]
  (let [id (:id (:path-params request))
        my-url (str "/post/" id)]
    (if-not (logged-in? request)
      (redirect my-url)
      (let [url (get (:form-params request) "url")
            txt (get (:form-params request) "txt")
            tags (get (:form-params request) "tags")
            editor (:uid (:session request))
            mreq (select-keys request [:server-port :scheme])
            posts (dbp/get-posts :some {:id id :author editor} mreq)]
        (if-not (pos? (first posts))
          (do
            (println "Failed updating [" id "], post not found")
            (redirect my-url))
          (let [result (dbp/update-post! {:url url :txt txt :tag (or tags "") :id id})]
            (-> (redirect my-url)
                (assoc :flash id))))))))

; simple pages
(defn render-page
  ([request page-name]              (layout/render2 request (str "page_" (name page-name) ".html")))
  ([request page-name data]         (layout/render2 request (str "page_" (name page-name) ".html") data)))

(defn render-page-404
  ([request] (render-page-404 request "The page could not be found."))
  ([request content] (layout/error-page {:status 404 :headers {"Content-Type" "text/html"} :message content})))

(defn posts-page [request & [modus]]
  (let [qp (merge (util/keywordize (:query-params request)) (:path-params request))
        mreq (select-keys request [:server-port :scheme])
        author (dbu/get-user-by-hostname {:hostname (:server-name request)} mreq)
        posts (dbp/get-posts :some (assoc qp :author (:uid author)) mreq)
        author2 (util/set-author author request)]
    (if (= :edit modus)
      (render-page request :edit_post (merge qp {:form (first (second posts)) :pcount (first posts) :post {:author (:author author2)}}))
      (render-page request :posts (merge qp {:modus modus :posts (second posts) :pcount (first posts) :post {:author (:author author2)}})))))

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
        qp (if (and (empty? (:txt qp)) (seq (:title qp))) (assoc qp :txt (util/sanitize-title (:title qp))) qp)
        cur-url (str (:uri request) "?" (:query-string request))]
  (println "add-page ror " (:session request) (:flash request))
  (println "add-page" (= :get (:request-method request)) (not (logged-in? request)) (empty? (:return (:session request))))
  (if (and (= :get (:request-method request)) (not (logged-in? request)) (empty? (:return (:session request))))
    (-> (redirect cur-url) (assoc-in [:session :return] cur-url))
    (render-page request :add {:form qp}))))

(defn home-page [request]
  (if-let [hostname (util/is-custom-host (:server-name request))]
    (posts-page request)
    (render-page request :index)))

(defn login-page [request]
  (println "login-page" (:session request))
  (render-page request :login))

(defn meta-page [request]
  (let [uid (or (:uid (:session request)) 0)
        profile (when (pos? uid) (dbu/get-profile {:uid uid} request))]
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
   ["/post/:id/edit" {:get (fn [{:keys [path-params query-params] :as req}] (posts-page req :edit))}]
   ["/post/:id/del"  {:post delete-item!}]
   ["/post/:id"      {:get (fn [{:keys [path-params query-params] :as req}] (posts-page req :single))
                      :post edit-item!}]
   ["/posts"    {:get posts-page}]
   ["/users"    {:get users-page}]
   ["/"         {:get home-page}]])
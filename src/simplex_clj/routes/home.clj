(ns simplex-clj.routes.home
  (:use compojure.core)
  (:require [simplex-clj.views.layout :as layout]
            [simplex-clj.util :as util]
            [simplex-clj.config :as config]
            [simplex-clj.models.schema :as schema]
            [simplex-clj.models.db :as db]))

; simple pages
(defn BLANK []
  (layout/render "page_blank.html"))

(defn home-page []
  (layout/render
    "page_home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page []
  (layout/render "page_about.html"))

; pages from DB
(defn- add-fields [coll]
  (let [info (util/video-info (:url coll))]
    (assoc coll :code (:code info), :site (:site info), :type (:itemtype coll))))

(defn show-single [id]
  (layout/render
   "page_posts.html" {:posts (map add-fields (db/get-post id))}))

(defn show-some
  ([n]
    (show-some n 0))
  ([n page]
    (layout/render
     "page_posts.html" {:posts (map add-fields (db/get-posts n (* n (dec page))))})))

; interaction
(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-name (:url params)) "." ext)
        x (util/download-file (:url params) (config/abs-file filename))
        sizes (util/image-size (config/abs-file filename))
        params (assoc params :id nil :itemtype (:type params) :meta (apply str (interpose ":" sizes)) :tag "foo" :created nil :updated nil :url (config/rel-file filename))]
    (schema/new-post (dissoc params :type))))

(defn store-text [params]
  (let [params (assoc params :itemtype (:type params) :tag "foo" :id nil :meta nil :created nil :updated nil)]
    (schema/new-post (dissoc params :type :url))))

(defn store-link [params]
  (let [params (assoc params :itemtype (:type params) :tag "foo" :id nil :meta nil :created nil :updated nil)]
    (schema/new-post (dissoc params :type))))


(defn store [params]
  (if (.equals "image" (:type params))
    (store-image params)
      (if (.equals "text" (:type params))
        (store-text params)
        (store-link params))))

(defn store-post [authkey params]
  (if
    (util/valid-authkey? authkey)
    (let [type (if (empty? (:type params)) (util/guess-type (:url params) (:txt params)) (:type params))
          params (assoc params :type type :author (util/user-from-authkey authkey))]
      (do
        (store params)
        (layout/render
          "page_justposted.html" {:content (interpose ":" (vals params))})))
    (BLANK)))

; fake, debug, test
(defn test-img []
  (let [url "http://dump.f5n.org/dump/4461aa9a5867480f4862084748ef29ff1cd366e4.jpeg"
        path "/home/florian/code/clojure/simplex-clj/resources/public/dump/"
        ext (util/file-extension url)
        newname (str (util/hash-name url) "." ext)
        x (util/download-file url (str path newname))
        sizes (util/image-size (str path newname))]
        (do
          (println url)
          (println newname)
          (println sizes))))

(defn test-args [r]
  (layout/render
    "page_blank.html" {:content (str (:url r) (:txt r) (:type r))}))

(defn show-single-fake [id]
  (layout/render
    "post_image.html" {:content (str "Foo: " id)
                       :created "08.06.2011 09:32"
                       :id (str id)
                       :url "http://dump.f5n.org/dump/4461aa9a5867480f4862084748ef29ff1cd366e4.jpeg"}))

(defn int-or-default [s default]
  (if
    (empty? s)
    default
    (try
      (let [n (Integer/parseInt s)]
        (if (pos? n) n default))
      (catch Exception e
        default))))

; ROUTES
(defroutes home-routes
  (GET "/test-args" [url txt type] (test-args {:url url :txt txt :type type}))
  (GET "/test-image" [] (test-img))
  (GET "/fake/:id" [id] (show-single-fake id))

  (GET "/store/:authkey" [url txt type authkey] (store-post authkey {:url url :txt txt :type type}))
  (GET "/show/:id" [id] (show-single id))
  (GET "/about" [] (about-page))
  (GET "/" [page limit] (show-some (int-or-default limit 10) (int-or-default page 1))))
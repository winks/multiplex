(ns simplex-clj.routes.home
  (:use compojure.core)
  (:require [simplex-clj.views.layout :as layout]
            [simplex-clj.util :as util]
            [simplex-clj.config :as config]
            [simplex-clj.models.schema :as db]))

; simple pages
(defn BLANK []
  (layout/render "blank.html"))

(defn home-page []
  (layout/render
    "home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page []
  (layout/render "about.html"))

; pages from DB
(defn show-single [id]
  (let [record (db/get-post id)
        info (util/video-info (:url record))]
    (layout/render
      (str "post_" (:itemtype record) ".html") (assoc record :code (:code info), :site (:site info)))))

(defn show-some [n]
  (let [records (db/get-posts n)]
    (layout/render
     "post_image.html")))

; interaction
(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-name (:url params)) "." ext)
        x (util/download-file (:url params) (config/abs-file filename))
        sizes (util/image-size (config/abs-file filename))
        params (assoc params :id nil :itemtype (:type params) :meta (apply str (interpose ":" sizes)) :tag "foo" :created nil :updated nil :url (config/rel-file filename))]
    (db/new-post (dissoc params :type))))

(defn store-text [params]
  (let [params (assoc params :itemtype (:type params) :tag "foo" :id nil :meta nil :created nil :updated nil)]
    (db/new-post (dissoc params :type :url))))

(defn store-link [params]
  (let [params (assoc params :itemtype (:type params) :tag "foo" :id nil :meta nil :created nil :updated nil)]
    (db/new-post (dissoc params :type))))


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
          "justposted.html" {:content (interpose ":" (vals params))})))
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
    "blank.html" {:content (str (:url r) (:txt r) (:type r))}))

(defn show-single-fake [id]
  (layout/render
    "post_image.html" {:content (str "Foo: " id)
                       :created "08.06.2011 09:32"
                       :id (str id)
                       :url "http://dump.f5n.org/dump/4461aa9a5867480f4862084748ef29ff1cd366e4.jpeg"}))

; ROUTES
(defroutes home-routes
  (GET "/test-args" [url txt type] (test-args {:url url :txt txt :type type}))
  (GET "/test-image" [] (test-img))
  (GET "/fake/:id" [id] (show-single-fake id))

  (GET "/store/:authkey" [url txt type authkey] (store-post authkey {:url url :txt txt :type type}))
  (GET "/some" [] (show-some 10))
  (GET "/show/:id" [id] (show-single id))
  (GET "/about" [] (about-page))
  (GET "/" [] (show-some 10)))

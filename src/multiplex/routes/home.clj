(ns multiplex.routes.home
  (:use compojure.core)
  (:require [clojure.data.json :as json]
            [multiplex.views.layout :as layout]
            [multiplex.util :as util]
            [multiplex.config :as config]
            [multiplex.models.db :as db]))

; simple pages
(defn form-fill
  [params]
    (if
      (and (empty? (:txt params)) (seq (:title params)))
      (assoc params :txt (:title params))
      params))

(defn BLANK []
  (layout/render "page_blank.html"))

(defn home-page []
  (layout/render
    "page_home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page [params]
  (layout/render "page_about.html" (db/get-user-by-key (:apikey params))))

(defn add-page [params]
  (if
    (db/valid-apikey? (:apikey params))
    (layout/render "page_add.html" (form-fill params))
    (layout/render "page_add.html" (assoc params :apikey ""))))

; pages from DB
(defn- add-fields [coll]
  (let [info (util/video-info (:url coll))]
    (assoc coll :code (:code info), :site (:site info))))

(defn show-single [id]
  (layout/render
   "page_posts.html" {:posts (map add-fields (db/get-post-by-id id))}))

(defn show-some
  ([n]
    (show-some n 0))
  ([n page]
    (let [posts (map add-fields (db/get-posts n (* n (dec page))))
          current (clojure.core/count posts)
          page-newer (when-not (< page 2) (dec page))
          page-older (when-not (< current n) (inc page))
          page-count (db/get-post-count)
          pages (range 1 (inc (/ (+ n (- page-count (mod page-count n))) n)))]
      (layout/render
       "page_posts.html" {:posts posts
                          :page-newer page-newer
                          :page-older page-older
                          :pages pages}))))

; interaction
(defn cleanup
  ([params]
    (cleanup params []))
  ([params what]
    (apply dissoc params (conj what :apikey :title))))

(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-filename (:url params)) "." ext)
        x (util/download-file (:url params) (config/abs-file filename))
        sizes (util/image-size (config/abs-file filename))
        params (assoc params :id nil
                             :meta (json/json-str {:size (clojure.string/join ":" sizes) :url (:url params)})
                             :tag "foo"
                             :created nil
                             :updated nil
                             :url (config/rel-file filename))]
    (db/new-post (cleanup params))))

(defn store-text [params]
  (let [params (assoc params :tag "foo"
                             :id nil
                             :meta ""
                             :created nil
                             :updated nil)]
    (db/new-post (cleanup params [:url]))))

(defn store-link [params]
  (let [params (assoc params :tag "foo"
                             :id nil
                             :meta ""
                             :created nil
                             :updated nil)]
    (db/new-post (cleanup params))))


(defn store [params]
  (if (.equals "image" (:imagetype params))
    (store-image params)
      (if (.equals "text" (:imagetype params))
        (store-text params)
        (store-link params))))

(defn store-post [apikey params]
  (if-let [user (db/get-user-by-key apikey)]
    (let [itemtype (if (empty? (:itemtype params)) (util/guess-type (:url params) (:txt params)) (:itemtype params))
          params (assoc params :itemtype itemtype :author (:uid user))]
      (do
        (store params)
        (layout/render
          "page_justposted.html" {:content (clojure.string/join ":" (vals params))} )))
    (BLANK)))


; fake, debug, test
(defn test-img []
  (let [url "http://dump.f5n.org/dump/4461aa9a5867480f4862084748ef29ff1cd366e4.jpeg"
        path "/home/florian/code/clojure/multiplex/resources/public/dump/"
        ext (util/file-extension url)
        newname (str (util/hash-filename url) "." ext)
        x (util/download-file url (str path newname))
        sizes (util/image-size (str path newname))]
    (println url)
    (println newname)
    (println sizes)))

(defn test-args [r]
  (layout/render
    "page_blank.html" {:content (str (:url r) (:txt r) (:itemtype r))}))

(defn show-single-fake [id]
  (layout/render
    "post_image.html" {:content (str "Foo: " id)
                       :created "08.06.2011 09:32"
                       :id (str id)
                       :url "http://dump.f5n.org/dump/4461aa9a5867480f4862084748ef29ff1cd366e4.jpeg"}))

(defn untaint
  ([url txt itemtype]
    (untaint url txt itemtype "" ""))
  ([url txt itemtype apikey]
    (untaint url txt itemtype apikey ""))
  ([url txt itemtype apikey title]
    {:url url
     :txt (clojure.string/trim txt)
     :itemtype itemtype
     :apikey apikey
     :title (clojure.string/trim title)}))

; ROUTES
(defroutes home-routes
  (GET "/test-args" [url txt type] (test-args (untaint url txt type)))
  (GET "/test-image" [] (test-img))
  (GET "/fake/:id" [id] (show-single-fake id))

  (POST "/store/:apikey" [url txt type apikey] (store-post apikey (untaint url txt type)))
  (GET "/add/:apikey" [url txt type apikey title] (add-page (untaint url txt type apikey title)))
  (GET "/show/:id" [id] (show-single id))
  (GET "/about" [] (about-page {}))
  (GET "/about/:apikey" [apikey] (about-page {:apikey apikey}))
  (GET "/" [page limit] (show-some (util/int-or-default limit 10) (util/int-or-default page 1))))

  ;(GET "/store/:apikey" [url txt type apikey] (store-post apikey {:url url :txt txt :type type}))

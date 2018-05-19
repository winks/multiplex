(ns multiplex.routes.home
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [noir.request :refer :all]
            [noir.response :as response]
            [clj-rss.core :as rss]
            [multiplex.config :as config]
            [multiplex.gfx :as gfx]
            [multiplex.models [user :as muser]
                              [post :as mpost]
                              [tag :as mtag]]
            [multiplex.util :as util]
            [multiplex.views.layout :as layout]))

(defn sanitize-title [s]
  (-> s
      (str/replace #"^▶ " "")
      (str/replace #" - YouTube$" "")
      (str/replace #" - MyVideo$" "")
      (str/replace #" on Vimeo$" "")))

(defn rssify [x]
  {:title (:txt x)
   :description (:url x)
   :pubDate (:created x)
   :category (:itemtype x)})

; parameter mangling
(defn form-fill
  [params]
    (if (and (empty? (:txt params)) (seq (:title params)))
      (assoc params :txt (sanitize-title (:title params)))
      params))

(defn prepare-page-add
  [params]
  (if (muser/valid-apikey? (:apikey params))
    (form-fill params)
    (assoc params :apikey "")))

(defn cleanup
  "add mandatory parameters like created/updated and remove unneeded ones"
  ([params]
    (cleanup params []))
  ([params what]
    (assoc (apply dissoc params (conj what :apikey)) :created nil :updated nil)))

; pages from DB
(defn show-single [id]
  (layout/render "page_posts.html" {:posts (map util/add-fields (mpost/get-post-by-id id))}))

(defn get-some
  [n page where-clause]
  (let [offset (* n (dec page))
        posts (map util/add-fields (mpost/get-posts n offset where-clause))
        post-count (mpost/get-post-count where-clause)
        pagination (util/calculate-pagination n page post-count)
        itemtype (:itemtype where-clause)]
      (assoc pagination
        :posts posts
        :page page
        :limit n
        :itemtype itemtype)))

(defn show-some
  ([n]
    (show-some n 0 {}))
  ([n page]
    (show-some n page {}))
  ([n page where-clause]
    (layout/render
      "page_posts.html" (get-some n page where-clause))))

; simple pages
(defn render-page
 ([page-name] (layout/render (str "page_" (name page-name) ".html")))
 ([page-name content html] (layout/render (str "page_" (name page-name) ".html") {:content content :html html})))

(defn render-page-404
 ([] (render-page-404 "The page could not be found."))
 ([content] {:status 404 :headers {"Content-Type" "text/html"} :body (layout/render "page_404.html" {:content content})}))

(defn render-page-user-x [params usr loggedin]
  (let [author (:uid usr)
        apikey (:apikey usr)
        avatar (util/get-avatar author)
        where-clause (if (mpost/valid-itemtype? (:itemtype params))
                         {:author author :itemtype (:itemtype params)}
                         {:author author})
        posts-map (get-some (:limit params) (:page params) where-clause)
        post (if loggedin
                (assoc (cleanup usr) :avatar avatar :username (:username usr) :apikey apikey)
                (assoc (cleanup usr) :avatar avatar :username (:username usr)))]
    (layout/render "page_user.html" (assoc posts-map :post post))))

(defn render-page-user [params]
  (if-let [usr (muser/get-user-by-key (:apikey params))]
    (render-page-user-x params usr true)
    (if-let [usr (muser/get-user-by-hostname (:hostname params))]
      (render-page-user-x params usr false)
      (render-page-404 "User does not exist."))))

(defn render-page-stream [params]
  (let [where-clause (if (mpost/valid-itemtype? (:itemtype params)) {:itemtype (:itemtype params)} {})]
    (show-some (:limit params) (:page params) where-clause)))

(defn render-page-add
  [params]
  (layout/render "page_add.html" (prepare-page-add params)))

(defn render-page-signup
  [params]
  (if-let [success (muser/create-user params)]
    (layout/render "page_signup.html" success)
    (layout/render "page_signup.html" params)))

; interaction
(defn prep-new
  [params]
  (let [params (assoc params :meta (json/write-str (:meta params)))]
    (mpost/new-post (cleanup params))))

(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-filename (:url params)) "." ext)
        abs-filename (config/abs-file filename)
        x (util/download-file (:url params) abs-filename)
        img (gfx/read-image abs-filename)
        sizes (gfx/image-size img)
        resized (gfx/calc-resized img)
        params (assoc params :id nil
                             :meta {:size (str/join ":" sizes) :url (:url params)}
                             :url (config/rel-file filename))]
    (do
      (println (str "store-image: " (:url params) sizes resized))
      (if-let [need (gfx/needs-resize? sizes resized abs-filename)]
        (let [r (gfx/resize img abs-filename (first resized) (second resized))
              params (assoc params :meta (assoc (:meta params) :thumb (util/file-extension abs-filename)
                                                               :thumbsize (str/join ":" resized)))]
          (prep-new params))
        (prep-new params)))))

(defn store-video-thumb [params]
  (let [vi       (util/video-info (:url params))
        thumb    (util/thumbnail-url vi)
        ext      (util/file-extension thumb)
        filename (str (:thumb-id vi) "." ext)
        abs-file (config/abs-file-thumb filename (:site vi))
        x        (util/download-file thumb abs-file)
        img      (gfx/read-image abs-file)
        resized  (gfx/calc-resized img)
        params   (assoc params :meta (assoc vi :thumbnail filename
                                               :thumbsize (str/join ":" resized)))]
    (prep-new params)))

(defn store-text [params]
  (let [params (assoc params :id nil
                             :meta "")]
    (println (str "store-text: " (:url params)))
    (mpost/new-post (cleanup params [:url]))))

(defn store-link-etc [params]
  (let [params (assoc params :id nil
                             :meta "")]
    (println (str "store-link-etc: " (:url params)))
    (mpost/new-post (cleanup params))))

(defn store-dispatch [params]
  (if (= "video" (:itemtype params))
    (store-video-thumb params)
    (if (= "audio" (:itemtype params))
      (store-video-thumb params)
      (if (= "image" (:itemtype params))
        (store-image params)
        (if (= "text" (:itemtype params))
          (store-text params)
          (store-link-etc params))))))

(defn render-page-store [apikey params]
  (if-let [user (muser/get-user-by-key apikey)]
    (let [itemtype (if (empty? (:itemtype params)) (util/guess-type (:url params) (:txt params)) (:itemtype params))
          params (assoc params :itemtype itemtype :author (:uid user))
          newid (store-dispatch params)
          post (mpost/get-post-by-id (:id newid))]
      (do
        (println params)
        (layout/render "page_justposted.html" {:post post
                                               :id (:id newid)
                                               :content (str "Saved as" (:id newid) "<br>")})))
    (render-page :blank)))

(defn render-rss [items title link description]
  (rss/channel-xml {:title title :link link :description description} items))

; dispatch
(defn untaint
  "TODO: this needs some real checks"
  ([url txt itemtype tags]
    (untaint url txt itemtype tags "" ""))
  ([url txt itemtype tags apikey]
    (untaint url txt itemtype tags apikey ""))
  ([url txt itemtype tags apikey title]
    {:url url
     :txt (str/trim txt)
     :itemtype itemtype
     :apikey apikey
     :tag (mtag/sanitize-tags tags)
     :title (str/trim title)}))

(defn untaint-signup
  "TODO: this needs some real checks"
  [username email password code]
  {:username (util/string-or-default username)
   :email (util/string-or-default email)
   :password  (util/string-or-default password)
   :signupcode (util/string-or-default code)})

(defroutes home-routes
  (POST "/store/:apikey" [url txt type apikey tags]
      (render-page-store apikey (untaint url txt type tags)))
  (POST "/signup" [username email password code]
    (if-let [username (util/is-subdomain)]
      (render-page-404)
      (render-page-signup (untaint-signup username email password code))))
  (GET  "/signup" [code]
    (if-let [hostname (util/is-custom-host)]
      (render-page-404)
      (render-page-signup {:code (util/string-or-default code "")})))
  (GET  "/add/:apikey" [url txt type tags apikey title]
    (render-page-add (untaint url txt type (util/string-or-default tags) apikey title)))
  (GET  "/post/:id" [id]
    (if-let [hostname (util/is-custom-host)]
      (show-single (util/int-or-default id 0))
      (render-page-404)))
  (GET  "/users" []
    (if-let [hostname (util/is-custom-host)]
      (render-page-404)
      (render-page :users (muser/get-public-users) "")))
  (GET  "/about" []
    (if-let [hostname (util/is-custom-host)]
      (render-page-404)
      (render-page :about)))
  (GET  "/about/changes" []
    (if-let [hostname (util/is-custom-host)]
      (render-page-404)
      (render-page :content "" (util/mdfile->html "/md/changes.md"))))
  (GET  "/everyone" [page limit type]
    (if-let [hostname (util/is-custom-host)]
      (render-page-404)
      (render-page-stream {:limit (util/int-or-default limit config/default-limit)
                           :page (util/int-or-default page 1)
                           :itemtype (util/string-or-default type)})))
  (GET  "/meta/:apikey" [apikey page limit type]
    (render-page-user {:limit (util/int-or-default limit config/default-limit)
                       :page (util/int-or-default page 1)
                       :itemtype (util/string-or-default type)
                       :apikey apikey}))
  (GET  "/rss" []
    (if-let [hostname (util/is-custom-host)]
      (let [author (muser/get-user-by-hostname hostname )
            posts (get-some 10 1 {:author (:uid author)})
            items (map rssify (:posts posts))]
        (render-rss
         items
          (:page-title config/multiplex)
          (util/make-url (:page-scheme config/multiplex) (:page-url config/multiplex) true)
          (:title author)))
      (render-rss
        []
        (:page-title config/multiplex)
        (util/make-url (:page-scheme config/multiplex) (:page-url config/multiplex) true)
        "")))
  (GET  "/" [page limit type]
    (if-let [hostname (util/is-custom-host)]
      (render-page-user {:hostname hostname
                         :limit (util/int-or-default limit config/default-limit)
                         :page (util/int-or-default page 1)
                         :itemtype (util/string-or-default type)})
      (render-page :index))))

(ns multiplex.routes.home
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [noir.request :refer :all]
            [noir.response :as response]
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
(defn BLANK []
  (layout/render "page_blank.html"))

(defn render-page-content [content html]
  (layout/render "page_content.html" {:content content :html html}))

(defn render-page-html [html]
  (render-page-content "" html))

(defn render-page-plain [content]
  (render-page-content content ""))

(defn home-page []
  (layout/render "page_home.html" {:content (util/mdfile->html "/md/docs.md")}))

(defn render-page-about []
  (layout/render "page_about.html"))

(defn render-page-index []
  (layout/render "page_index.html"))

(defn render-page-user-x [params usr loggedin]
  (let [author (:uid usr)
        apikey (:apikey usr)
        avatar (util/get-avatar author)
        where-clause (if (mpost/valid-itemtype? (:itemtype params))
                         {:author author :itemtype (:itemtype params)}
                         {:author author})
        posts-map (get-some (:limit params) (:page params) where-clause)
        post (if loggedin
                (assoc (cleanup usr) :avatar avatar :apikey apikey)
                (assoc (cleanup usr) :avatar avatar))]
    (layout/render "page_user.html" (assoc posts-map :post post))))

(defn render-page-user [params]
  (if-let [usr (muser/get-user-by-key (:apikey params))]
    (render-page-user-x params usr true)
    (if-let [usr (muser/get-user-by-hostname (:hostname params))]
      (render-page-user-x params usr false)
      (render-page-plain "User does not exist."))))

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
      (println (str "xxx: " sizes resized))
      (println (str "store-image: " (:url params)))
        (if-let [need (gfx/needs-resize? sizes resized abs-filename)]
          (let [r (gfx/resize img abs-filename (first resized) (second resized))
                params (assoc params :meta (assoc (:meta params) :thumb (util/file-extension abs-filename)
                                                                 :thumbsize (str/join ":" resized)))]
            (prep-new params))
            (prep-new params)))))

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
  (if (= "image" (:itemtype params))
    (store-image params)
      (if (= "text" (:itemtype params))
        (store-text params)
        (store-link-etc params))))

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
    (BLANK)))

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
      (response/status 404 "your page could not be found")
      (render-page-signup (untaint-signup username email password code))))
  (GET  "/signup" [code]
    (if-let [hostname (util/is-custom-host)]
      (response/status 404 "your page could not be found")
      (render-page-signup {:code (util/string-or-default code "")})))
  (GET  "/add/:apikey" [url txt type tags apikey title]
    (render-page-add (untaint url txt type (util/string-or-default tags) apikey title)))
  (GET  "/post/:id" [id]
    (if-let [hostname (util/is-custom-host)]
      (show-single (util/int-or-default id 0))
      (response/status 404 "your page could not be found")))
  (GET  "/about" []
    (if-let [hostname (util/is-custom-host)]
      (response/status 404 "your page could not be found")
      (render-page-about)))
  (GET  "/about/changes" []
    (if-let [hostname (util/is-custom-host)]
      (response/status 404 "your page could not be found")
      (render-page-html (util/mdfile->html "/md/changes.md"))))
  (GET  "/everyone" [page limit type]
    (if-let [hostname (util/is-custom-host)]
      (response/status 404 "your page could not be found")
      (render-page-stream {:limit (util/int-or-default limit config/default-limit)
                           :page (util/int-or-default page 1)
                           :itemtype (util/string-or-default type)})))
  (GET  "/meta/:apikey" [apikey page limit type]
    (render-page-user {:limit (util/int-or-default limit config/default-limit)
                       :page (util/int-or-default page 1)
                       :itemtype (util/string-or-default type)
                       :apikey apikey}))
  (GET  "/" [page limit type]
    (if-let [hostname (util/is-custom-host)]
      (render-page-user {:hostname hostname
                         :limit (util/int-or-default limit config/default-limit)
                         :page (util/int-or-default page 1)
                         :itemtype (util/string-or-default type)})
      (render-page-index))))

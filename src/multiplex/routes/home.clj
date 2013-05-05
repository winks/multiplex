(ns multiplex.routes.home
  (:use compojure.core
        [noir.response :only (redirect)])
  (:require [clojure.data.json :as json]
            [multiplex.config :as config]
            [multiplex.gfx :as gfx]
            [multiplex.models.user :as muser]
            [multiplex.models.post :as mpost]
            [multiplex.util :as util]
            [multiplex.views.layout :as layout]))

; parameter mangling
(defn form-fill
  [params]
    (if
      (and (empty? (:txt params)) (seq (:title params)))
      (assoc params :txt (:title params))
      params))

(defn prepare-page-add
  [params]
  (if
    (muser/valid-apikey? (:apikey params))
    (form-fill params)
    (assoc params :apikey "")))

(defn cleanup
  "add mandatory parameters like created/updated and remove unneeded ones"
  ([params]
    (cleanup params []))
  ([params what]
    (assoc (apply dissoc params (conj what :apikey :title)) :created nil :updated nil)))

; simple pages
(defn BLANK []
  (layout/render "page_blank.html"))

(defn home-page []
  (layout/render "page_home.html" {:content (util/md->html "/md/docs.md")}))

(defn render-page-about []
  (layout/render "page_about.html"))

(defn render-page-user [params]
  (if-let [usr (muser/get-user-by-key (:apikey params))]
   (layout/render "page_user.html" usr)
   (layout/render "page_user.html" (cleanup (muser/get-user-by-name (:username params))))))

(defn render-page-add
  [params]
  (layout/render "page_add.html" (prepare-page-add params)))

(defn render-page-signup
  [params]
  (if-let [success (muser/create-user params)]
    (layout/render "page_signup.html" success)
    (layout/render "page_signup.html" params)))

; pages from DB
(defn show-single [id]
  (layout/render "page_posts.html" {:posts (map util/add-fields (mpost/get-post-by-id id))}))

(defn show-some
  ([n]
    (show-some n 0))
  ([n page]
    (let [posts (map util/add-fields (mpost/get-posts n (* n (dec page))))
          current (clojure.core/count posts)
          page-newer (when-not (< page 2) (dec page))
          page-older (when-not (< current n) (inc page))
          page-count (mpost/get-post-count)
          pages (range 1 (inc (/ (+ n (- page-count (mod page-count n))) n)))]
      (layout/render
       "page_posts.html" {:posts posts
                          :page-newer page-newer
                          :page-older page-older
                          :pages pages}))))

; interaction

(defn store-image [params]
  (let [ext (util/file-extension (:url params))
        filename (str (util/hash-filename (:url params)) "." ext)
        abs-filename (config/abs-file filename)
        x (util/download-file (:url params) abs-filename)
        img (gfx/read-image abs-filename)
        sizes (gfx/image-size img)
        resized (gfx/calc-resized img)
        params (assoc params :id nil
                             :meta {:size (clojure.string/join ":" sizes) :url (:url params)}
                             :tag "foo"
                             :url (config/rel-file filename))]
    (do
      (println (str "xxx: " sizes resized))
      (if
        (not= sizes resized)
        (do
          (gfx/resize img abs-filename (first resized) (second resized))
          (assoc params :meta (assoc (:meta params) :thumb (util/file-extension abs-filename))))
        nil)
      (println (str "store-image: " (:url params)))
      (let [params (assoc params :meta (json/write-str (:meta params)))]
        (mpost/new-post (cleanup params))))))

(defn store-text [params]
  (let [params (assoc params :tag "foo"
                             :id nil
                             :meta "")]
    (println (str "store-text: " (:url params)))
    (mpost/new-post (cleanup params [:url]))))

(defn store-link-etc [params]
  (let [params (assoc params :tag "foo"
                             :id nil
                             :meta "")]
    (println (str "store-link-etc: " (:url params)))
    (mpost/new-post (cleanup params))))

(defn store-dispatch [params]
  (if (.equals "image" (:itemtype params))
    (store-image params)
      (if (.equals "text" (:itemtype params))
        (store-text params)
        (store-link-etc params))))

(defn render-page-store [apikey params]
  (if-let [user (muser/get-user-by-key apikey)]
    (let [itemtype (if (empty? (:itemtype params)) (util/guess-type (:url params) (:txt params)) (:itemtype params))
          params (assoc params :itemtype itemtype :author (:uid user))]
      (do
        (println params)
        (store-dispatch params)
        (layout/render "page_justposted.html" {:content (clojure.string/join ":" (vals params))} )))
    (BLANK)))

; dispatch
(defn untaint
  "TODO: this needs some real checks"
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

(defn untaint-signup
  "TODO: this needs some real checks"
  [username email password code]
  {:username (util/string-or-default username)
   :email (util/string-or-default email)
   :password  (util/string-or-default password)
   :signupcode (util/string-or-default code)})

(defroutes home-routes
  (POST "/store/:apikey" [url txt type apikey] (render-page-store apikey (untaint url txt type)))
  (POST "/signup" [username email password code] (render-page-signup (untaint-signup username email password code)))
  (GET "/signup" [code] (render-page-signup {:code (util/string-or-default code "")}))
  (GET "/add/:apikey" [url txt type apikey title] (render-page-add (untaint url txt type apikey title)))
  (GET "/post/:id" [id] (show-single id))
  (GET "/show/:id" [id] (redirect (str "/post/" id) :permanent))
  (GET "/about" [] (render-page-about))
  (GET "/user/:username/:apikey" [username apikey] (render-page-user {:username username :apikey apikey}))
  (GET "/user/:username" [username] (render-page-user {:username username}))
  (GET "/" [page limit] (show-some (util/int-or-default limit 10) (util/int-or-default page 1))))

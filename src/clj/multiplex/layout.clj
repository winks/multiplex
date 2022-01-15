(ns multiplex.layout
  (:require
    [clojure.java.io :as io]
    [clojure.string :as cstr]
    [markdown.core :refer [md-to-html-string]]
    [multiplex.config :as config]
    [multiplex.db.users :as dbu]
    [multiplex.util :as util]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [ring.util.http-response :refer [content-type ok]]
    [ring.util.response]
    [selmer.parser :as parser]
    [selmer.filters :as filters]))

(def ^:dynamic *pom-config*
  (with-open [pom-properties-reader (io/reader (io/resource "META-INF/maven/multiplex/multiplex/pom.properties"))]
    (doto (java.util.Properties.)
      (.load pom-properties-reader))))

(defn get-version [^java.util.Properties prop]
  (if (empty? prop)
    ""
    (let [ver (.getProperty prop "version")
          rev (.getProperty prop "revision")]
      (if (cstr/ends-with? ver "-SNAPSHOT")
        (str (cstr/replace ver "-SNAPSHOT" "") "-dev " (subs rev 0 7))
        ver))))

(parser/set-resource-path!  (io/resource "html"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(filters/add-filter! :markdown (fn [content] [:safe (md-to-html-string content)]))

(def config-fallback {:site-title "multiplex"
                      :site-url ""
                      :site-scheme :http
                      :site-port 80
                      :site-theme "default"
                      :assets-url ""})

(defn phelper [type params]
  (let [p (util/int-or (:page params) 1)
        l (util/int-or (:limit params) config/default-limit)]
    (util/type-pagination type p l)))

(defn prepare-params-author [request & [params]]
  (let [mreq (select-keys request [:server-port :scheme])
        author (dbu/get-user-by-hostname {:hostname (:server-name request)} mreq)]
    (:author (util/set-author author request))))

(defn prepare-params-glob [request & [params]]
  (let [authr (if (empty? (:author (:post params)))
                (prepare-params-author request params)
                (:author (:post params)))
        favicon       (first (remove empty? [(str (:uid authr)) "0"]))
        cfg           (first (remove empty? [(config/env :multiplex) config-fallback]))
        theme         (first (remove empty? [(:theme authr) (:site-theme cfg)]))
        assets-prefix (:assets-url cfg)
        site-title    (if-let [title (:title authr)] title (:site-title cfg))
        page-header   (if-let [title (:title authr)] title (if-let [u (:username authr)] (str u "'s multiplex") (:site-title cfg)))]
    {:page-header page-header
     :site-title site-title
     :theme theme
     :favicon favicon
     :modus (name (or (:modus params) ""))
     :assets-prefix assets-prefix
     :base-url (util/make-url (:site-url cfg) cfg)
     :version-string (get-version *pom-config*)
     :flash (:flash request)}))

(defn prepare-params [request & [params]]
  (let [; TODO refactor? done in get-posts already
        itemtype      (util/string-or (get params :type))
        limit         (util/int-or (get params :limit) config/default-limit)
        page          (util/int-or (get params :page) 1)

        pcount        (util/int-or (get params :pcount) 0)
        pagina        (util/calculate-pagination limit page pcount)
        auth-user     (:user (:session request))
        auth-uid      (:uid (:session request))
        auth-loggedin (some? auth-user)]
    (assoc (select-keys params [:form :post :posts :profile :users])
          :auth { :loggedin auth-loggedin
                  :user auth-user
                  :uid auth-uid}
          :glob (prepare-params-glob request params)
          :navi { :type-link (phelper "link" params)
                  :type-text (phelper "text" params)
                  :type-image (phelper "image" params)
                  :type-audio (phelper "audio" params)
                  :type-video (phelper "video" params)}
          :pagi (assoc pagina :type itemtype))))

(defn render-orig
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :page template
          :csrf-token *anti-forgery-token*)))
    "text/html; charset=utf-8"))

(defn render
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (let [p2 (prepare-params request params)]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc p2 :page template :csrf-token *anti-forgery-token*)))
    "text/html; charset=utf-8")))

(defn error-page
  "error-details should be a map containing the following keys:
   :status - error status
   :title - error title (optional)
   :message - detailed error message (optional)

   returns a response map with the error page as the body
   and the status specified by the status key"
  [error-details]
  {:status  (:status error-details)
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    (parser/render-file "page_error.html" error-details)})

(ns multiplex.layout
  (:require
    [multiplex.config :as config]
    [multiplex.util :as util]
    [multiplex.db.users :as dbu]
    [clojure.java.io]
    [selmer.parser :as parser]
    [selmer.filters :as filters]
    [markdown.core :refer [md-to-html-string]]
    [ring.util.http-response :refer [content-type ok]]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.util.response]))

(parser/set-resource-path!  (clojure.java.io/resource "html"))
(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))
(filters/add-filter! :markdown (fn [content] [:safe (md-to-html-string content)]))

(def config-fallback {:page-title "multiplex"
                      :page-url ""
                      :page-scheme "http"
                      :assets-scheme "http"
                      :theme "default"})

(defn phelper [type params]
  (let [p (util/int-or (:page params) 1)
        l (util/int-or (:limit params) config/default-limit)]
    (util/type-pagination type p l)))

(defn render
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

(defn render2
  "renders the HTML template located relative to resources/html"
  [request template & [params]]
  (let [
        ; :post {:author (:author author2)}
        aux (if (empty? (:author (:post params)))
              (let [mreq (select-keys request [:server-port :scheme])
                    author (dbu/get-user-by-hostname {:hostname (:server-name request)} mreq)
                    author2 (util/set-author author request)]
                (:author author2))
              (:author (:post params)))

        ;authr         (:author (:post params))
        authr aux
        theme         (first (filter seq [(:theme authr) (:theme (config/env :multiplex)) (:theme config-fallback)]))
        cfg           (assoc (first (filter seq [(config/env :multiplex) config-fallback])) :theme theme)
        assets-prefix (if-let [site (:assets-url cfg)] (util/make-url (:assets-scheme cfg) site) "")
        page-title    (if-let [x (:title authr)] x (:page-title cfg))
        page-header   (if-let [x (:title authr)] x (str (:username authr) "'s multiplex" ))
        ; TODO refactor? done in get-posts already
        itemtype      (util/string-or (get params :type))
        limit         (util/int-or (get params :limit) config/default-limit)
        page          (util/int-or (get params :page) 1)

        pcount        (util/int-or (get params :pcount) 0)
        pagina        (util/calculate-pagination limit page pcount)
        auth-user     (:user (:session request))
        auth-uid      (:uid (:session request))
        auth-loggedin (some? auth-user)]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :auth { :loggedin auth-loggedin
                  :user auth-user
                  :uid auth-uid}
          :glob { :page-header page-header
                  :page-title page-title
                  :theme theme
                  :assets-prefix assets-prefix
                  :base-url (util/make-url (:page-scheme cfg) (:page-url cfg) request)
                  :flash (:flash request)}
          :navi { :type-link (phelper "link" params)
                  :type-text (phelper "text" params)
                  :type-image (phelper "image" params)
                  :type-audio (phelper "audio" params)
                  :type-video (phelper "video" params)}
          :pagi (assoc pagina :limit limit :page page :type itemtype)
          :page template
          :csrf-token *anti-forgery-token*)))
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

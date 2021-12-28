(ns multiplex.layout
  (:require
    [multiplex.config :as config]
    [multiplex.util :as util]
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
                      :bookmark-text "Bookmark me"
                      :theme "default"})

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
  (let [theme (first (filter seq [(:theme (:post params)) (:theme (config/env :multiplex)) (:theme config-fallback)]))
        cfg           (assoc (first (filter seq [(config/env :multiplex) config-fallback])) :theme theme)
        assets-prefix (if-let [site (:assets-url cfg)] (util/make-url (:assets-scheme cfg) site) "")
        page-title    (if-let [x (:title (:post params))] x (:page-title cfg))
        page-header   (if-let [x (:title (:post params))] x (str (:username (:post params)) "'s multiplex" ))]
  (content-type
    (ok
      (parser/render-file
        template
        (assoc params
          :page-header page-header
          :page-title page-title
          :theme theme
          :assets-prefix assets-prefix
          :base-url (util/make-url (:page-scheme cfg) (:page-url cfg))
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
   :body    (parser/render-file "error.html" error-details)})

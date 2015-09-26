(ns multiplex.views.layout
  (:use noir.request)
  (:require [selmer.parser :as parser]
            [multiplex.config :as config]
            [multiplex.util :as util]))

(def template-path "multiplex/views/templates/")

(def config-fallback {:page-title "multiplex"
                      :page-url ""
                      :page-scheme "http"
                      :bookmark-text "Bookmark me"
                      :theme "default"})

(defn render [template & [params]]
  (let [uid (util/int-or-default (:uid (:post params)) 0)
        theme (if (> uid 0)(:theme (nth config/user-data uid)) (:theme config-fallback))
        cfg (assoc (or config/multiplex config-fallback) :theme theme)
        user-link (if-let [host (util/is-custom-host)] host (:page-url config/multiplex))
        page-title (if-let [x (:title (:post params))] x (:page-title cfg))
        page-header (if-let [x (:title (:post params))] x (str (:username (:post params)) "'s multiplex" ))]
    (parser/render-file (str template-path template)
                        (assoc params :context (:context *request*)
                                      :page-title page-title
                                      :page-header page-header
                                      :user-link "/"
                                      :multiplex cfg))))


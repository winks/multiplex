(ns multiplex.views.layout
  (:require [noir.request :refer :all]
            [ring.util.anti-forgery :refer :all]
            [selmer.parser :as parser]
            [multiplex.config :as config]
            [multiplex.util :as util]))

(def template-path "multiplex/views/templates/")

(def config-fallback {:page-title "multiplex"
                      :page-url ""
                      :page-scheme "http"
                      :bookmark-text "Bookmark me"
                      :theme "default"})

(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))

(defn render [template & [params]]
  (let [uid (util/int-or-default (:uid (:post params)) 0)
        theme (if (> uid 0)(:theme (nth config/user-data uid)) (:theme config-fallback))
        cfg (assoc (or config/multiplex config-fallback) :theme theme)
        user-link (if-let [host (util/is-custom-host)] host (:page-url config/multiplex))
        type-navi-link (util/type-pagination "link" (:page params) (:limit params))
        type-navi-text (util/type-pagination "text" (:page params) (:limit params))
        type-navi-image (util/type-pagination "image" (:page params) (:limit params))
        type-navi-audio (util/type-pagination "audio" (:page params) (:limit params))
        type-navi-video (util/type-pagination "video" (:page params) (:limit params))
        page-title (if-let [x (:title (:post params))] x (:page-title cfg))
        page-header (if-let [x (:title (:post params))] x (str (:username (:post params)) "'s multiplex" ))]
    (parser/render-file (str template-path template)
                        (assoc params :context (:context *request*)
                                      :page-header page-header
                                      :page-title page-title
                                      :type-navi-link type-navi-link
                                      :type-navi-text type-navi-text
                                      :type-navi-image type-navi-image
                                      :type-navi-audio type-navi-audio
                                      :type-navi-video type-navi-video
                                      :user-link "/"
                                      :multiplex cfg))))

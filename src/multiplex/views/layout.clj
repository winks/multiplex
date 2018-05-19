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
                      :assets-scheme "http"
                      :bookmark-text "Bookmark me"
                      :theme "default"})

(parser/add-tag! :csrf-field (fn [_ _] (anti-forgery-field)))

(defn phelper [type params]
  (let [p (util/int-or-default (:page params) 1)
        l (util/int-or-default (:limit params) config/default-limit)]
    (util/type-pagination type p l)))

(defn render [template & [params]]
  (let [uid (util/int-or-default (:uid (:post params)) 0)
        theme         (if-let [x (:theme (:post params))] x (or (:theme config/multiplex) (:theme config-fallback)))
        cfg           (assoc (or config/multiplex config-fallback) :theme theme)
        assets-prefix (if-let [site (:assets-url cfg)] (util/make-url (:assets-scheme cfg) site false) "")
        page-title    (if-let [x (:title (:post params))] x (:page-title cfg))
        page-header   (if-let [x (:title (:post params))] x (str (:username (:post params)) "'s multiplex" ))]
    (parser/render-file (str template-path template)
                        (assoc params :context (:context *request*)
                                      :page-header page-header
                                      :page-title page-title
                                      :theme theme
                                      :assets-prefix assets-prefix
                                      :base-url (util/make-url (:page-scheme cfg) (:page-url cfg) true)
                                      :type-navi-link (phelper "link" params)
                                      :type-navi-text (phelper "text" params)
                                      :type-navi-image (phelper "image" params)
                                      :type-navi-audio (phelper "audio" params)
                                      :type-navi-video (phelper "video" params)
                                      :multiplex cfg))))

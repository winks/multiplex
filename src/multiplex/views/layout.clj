(ns multiplex.views.layout
  (:use noir.request)
  (:require [clabango.parser :as parser]
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
        theme (if (> uid 0)(:theme (nth config/user-data uid)) (:theme config-fallback))]
    (parser/render-file (str template-path template)
                        (assoc params :context (:context *request*)
                                      :multiplex (assoc (or config/multiplex config-fallback) :theme theme)))))


(ns multiplex.views.layout
  (:use noir.request)
  (:require [clabango.parser :as parser]
            [multiplex.config :as config]))

(def template-path "multiplex/views/templates/")

(defn render [template & [params]]
  (parser/render-file (str template-path template)
                      (assoc params :context (:context *request*)
                                    :multiplex (or config/multiplex {:page-title "multiplex"
                                                                     :page-url ""
                                                                     :page-scheme "http"
                                                                     :bookmark-text "Bookmark me"}))))


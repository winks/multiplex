(ns simplex-clj.views.layout
  (:use noir.request)
  (:require [clabango.parser :as parser]))

(def template-path "simplex_clj/views/templates/")

(defn render [template & [params]]
  (parser/render-file (str template-path template)
                      (assoc params :context (:context *request*))))


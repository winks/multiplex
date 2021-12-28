(ns multiplex.db.posts
  (:require
   [multiplex.db.core :as db]
   [multiplex.util :as util]))

(defn get-posts [author]
(println (:uid author))
  (if (nil? (:uid author))
  []
    (let [orig (db/get-posts {:author (:uid author)})
          o2 (map #(util/add-fields % author) orig)]
    (println (first orig))
    (println author)
    (println "")
    (println (first o2))
    (println "")
  o2)))
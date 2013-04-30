(ns simplex-clj.config)

(def img-types '("jpg" "png" "gif" "svg"))
(def sites-youtube '("youtube.com" "youtu.be"))
(def sites-vimeo '("vimeo.com"))
(def sites-myvideo '("myvideo.de"))
(def sites-video (flatten (conj sites-youtube sites-vimeo sites-myvideo)))

(def abs-path "/home/florian/code/clojure/simplex-clj/resources/public/dump")
(def rel-path "/dump")

(defn abs-file [file] (str abs-path "/" file))
(defn rel-file [file] (str rel-path "/" file))

(def mydb (or (System/getenv "CLEARDB_DATABASE_URL") "mysql://simplex:simplex@127.0.0.1/simplex"))

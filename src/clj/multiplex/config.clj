(ns multiplex.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))

(defstate env
  :start
  (load-config
    :merge
    [(args)
     (source/from-system-props)
     (source/from-env)]))

(def itemtypes ["image" "link" "text" "video" "audio"])
(def default-limit 10)

(def img-types ["jpg" "png" "gif" "svg" "jpeg"])
(def img-max-size [640 480])

(def sites-youtube ["youtube.com" "youtu.be"])
(def sites-vimeo ["vimeo.com"])
(def sites-myvideo ["myvideo.de"])
(def sites-video (flatten (conj sites-youtube sites-vimeo sites-myvideo)))

(def sites-soundcloud ["soundcloud.com" "snd.sc"])
(def sites-mixcloud ["mixcloud.com"])
(def sites-audio (flatten (conj sites-soundcloud sites-mixcloud)))

(def sites-imgur-gifv ["imgur.com" "i.imgur.com"])

(defn abs-file-thumb [file site] (str (:content-abs-path (env :multiplex)) "/" site "/" site "." file))
(defn abs-file       [file]      (str (:content-abs-path (env :multiplex)) "/" file))
(defn rel-file       [file]      (str (:content-rel-path (env :multiplex)) "/" file))

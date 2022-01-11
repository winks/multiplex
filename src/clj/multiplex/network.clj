(ns multiplex.util
  (:require
    [clojure.java.io :as cjio]))

(defn download-file
  "copies an image from an URL to a local file"
  [url filename]
  (with-open [input (cjio/input-stream url)
              output (cjio/output-stream filename)]
    (cjio/copy input output)))

(defn read-remote
  "read from remote url"
  [url default]
  (try
    (slurp url)
    (catch Exception ex default)))
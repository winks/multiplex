(ns multiplex.test.util
  (:require [multiplex.config :as config])
  (:use [multiplex.util])
  (:use [clojure.test]))

(deftest test-video-info-imgur-1
  (let [info (video-info "http://imgur.com/foo.gifv")]
    (is (= "imgur-gifv" (:site info)))
    (is (= "foo" (:code info)))))

(deftest test-video-info-imgur-2
  (let [info (video-info "https://imgur.com/foo.gifv")]
    (is (= "imgur-gifv" (:site info)))
    (is (= "foo" (:code info)))))

(deftest test-video-info-imgur-3
  (let [info (video-info "http://i.imgur.com/foo.gifv")]
    (is (= "imgur-gifv" (:site info)))
    (is (= "foo" (:code info)))))

(deftest test-video-info-imgur-4
  (let [info (video-info "https://i.imgur.com/foo.gifv")]
    (is (= "imgur-gifv" (:site info)))
    (is (= "foo" (:code info)))))

(deftest test-video-info-youtube-1
  (let [info (video-info "https://www.youtube.com/watch?v=hbEM9Sr9fi4&feature=youtu.be")]
    (is (= "youtube" (:site info)))
    (is (= "hbEM9Sr9fi4" (:code info)))))

(deftest test-video-info-youtube-2
  (let [info (video-info "http://www.youtube.com/watch?v=_hbEM9Sr9fi4")]
    (is (= "youtube" (:site info)))
    (is (= "_hbEM9Sr9fi4" (:code info)))))

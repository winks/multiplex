(ns multiplex.util-test
  (:require
    [clojure.test :refer :all]
    [multiplex.config :as config]
    [multiplex.util :refer :all]))

; these are actually in multiplex.config
(deftest test-config-rel-file-1
  (is (= (str (:content-rel-path (config/env :multiplex)) "/foo2.jpg") (config/rel-file "foo2.jpg"))))
(deftest test-config-abs-file-1
  (is (= (str (:content-abs-path (config/env :multiplex)) "/foo1.jpg") (config/abs-file "foo1.jpg"))))
(deftest test-config-abs-file-thumb-1
  (is (= (str (:content-abs-path (config/env :multiplex)) "/testsite/foo0.jpg") (config/abs-file-thumb "foo0.jpg" "testsite"))))

; now multiplex.util
(deftest test-is-custom-host-1
  (is (= "example.f5n.de" (is-custom-host "example.f5n.de")))
  (is (= false (is-custom-host (:site-url (config/env :multiplex))))))

; download-file

(deftest test-valid-post-type-1
  (is (= false (valid-post-type? "")))
  (is (= nil (valid-post-type? "foobar")))
  (is (= true (valid-post-type? "video"))))

(deftest test-valid-tag-1
  (is (= "asd" (valid-tag? "asd")))
  (is (= "asd" (valid-tag? "ASD")))
  (is (= nil (valid-tag? "asd+")))
  (is (= nil (valid-tag? "asd\"asd")))
  (is (= nil (valid-tag? ""))))

(deftest test-file-extension-1
  (is (= "" (file-extension "")))
  (is (= "gif" (file-extension "test.gif")))
  (is (= "m3u" (file-extension "test.m3u")))
  (is (= "jpg" (file-extension "test.jpeg"))))

(deftest test-make-url-1
  (is (= "http://example.org" (make-url "example.org" {})))
  (is (= "https://example.org" (make-url "example.org" {:site-scheme :https})))
  (is (= "http://example.org:81" (make-url "example.org" {:site-port 81})))
  (is (= "http://example.org" (make-url "example.org" {:site-port 0})))
  (is (= "https://example.org:444" (make-url "example.org" {:site-scheme :https :site-port 444}))))

(deftest test-host-name-1
  (is (= "example.org" (host-name "http://example.org")))
  (is (= "example.org" (host-name "https://example.org")))
  (is (= "example.org" (host-name "https://www.example.org")))
  (is (= "sub.example.org" (host-name "https://sub.example.org/path"))))

; read-remote

(deftest test-video-info-err
  (let [info (video-info "http://example.com/foo.gif")]
    (is (= "err" (:site info)))
    (is (= "" (:code info)))))

(deftest test-video-info-imgur-1
  (let [info (video-info "https://imgur.com/foo.gifv")]
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

; TODO not a unit test, IO
(deftest test-video-info-vimeo-1
  (let [info (video-info "https://vimeo.com/62518619")]
    (is (= "vimeo" (:site info)))
    (is (= "62518619" (:code info)))))

(deftest test-video-info-mixcloud-1
  (let [info (video-info "https://www.mixcloud.com/ZorrinooohH/februar-2019-retrospective/")]
    (is (= "mixcloud" (:site info)))
    (is (= "%2FZorrinooohH%2Ffebruar-2019-retrospective%2F" (:code info)))))

; TODO not a unit test, IO
(deftest test-video-info-soundcloud-1
  (let [info (video-info "https://soundcloud.com/zuckerton-records/klangkuenstler-munich-motion")]
    (is (= "soundcloud" (:site info)))
    (is (= "70598455" (:code info)))))

(deftest test-thumbnail-url-1
  (is (= "" (thumbnail-url nil)))
  (is (= "" (thumbnail-url {})))
  (is (= "" (thumbnail-url {:site "foo"})))
  (is (= "https://i.ytimg.com/vi/1234/hqdefault.jpg" (thumbnail-url {:site "youtube" :code 1234})))
  (is (= "https://i.vimeocdn.com/video/1234.jpg?mw=480" (thumbnail-url {:site "vimeo" :thumb-id 1234})))
  (is (= "/p/1234.pdf" (thumbnail-url {:site "soundcloud" :thumb-path "/p/" :thumb-id 1234 :thumb-ext "pdf"}))))

(deftest test-string-or-1
  (is (= "" (string-or nil)))
  (is (= "" (string-or "")))
  (is (= "" (string-or " ")))
  (is (= "3" (string-or "3")))
  (is (= "3" (string-or " 3")))
  (is (= "3" (string-or "3 "))))

(deftest test-string-or-2
  (is (= "x" (string-or nil "x")))
  (is (= "x" (string-or "" "x"))))

(deftest test-int-or-1
  (is (= 23 (int-or nil 23)))
  (is (= 23 (int-or "nil" 23)))
  (is (= 23 (int-or {:id 0} 23)))
  (is (= 23 (int-or [0] 23)))
  (is (= 42 (int-or "42" 23)))
  (is (= 42 (int-or (int 42) 23)))
  (is (= 42 (int-or (long 42) 23)))
  (is (= 42 (int-or (double 42) 23)))
  (is (= 23 (int-or -42 23))))

;(deftest test-add-fields)
; set-author

(deftest test-keywordize-1
  (is (= {} (keywordize {})))
  (is (= {:foo1 "bar1" :foo2 "bar2"} (keywordize {"foo1" "bar1" "foo2" "bar2"}))))

; calculate-pagination

; type-pagination

(deftest test-sanitize-title-1
  (is (= "" (sanitize-title "")))
  (is (= "foo" (sanitize-title "foo - YouTube")))
  (is (= "foo" (sanitize-title "foo - MyVideo")))
  (is (= "foo" (sanitize-title "foo on Vimeo")))
  (is (= "foo" (sanitize-title "▶ foo"))))

(deftest test-guess-type-1
  (is (= "text" (guess-type "" "url")))
  (is (= "text" (guess-type nil "url")))
  (is (= "video" (guess-type "https://youtube.com/foo" "asdf")))
  (is (= "video" (guess-type "https://youtu.be/foo" "asdf")))
  (is (= "video" (guess-type "https://vimeo.com/foo" "asdf")))
  (is (= "video" (guess-type "https://myvideo.de/foo" "asdf")))
  (is (= "audio" (guess-type "https://soundcloud.com/foo" "asdf")))
  (is (= "image" (guess-type "https://example.org/foo.png" "asdf")))
  (is (= "link" (guess-type "https://example.org/foo" "asdf"))))
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
  (is (= (str (:content-abs-path (config/env :multiplex)) "/testsite.foo0.jpg") (config/abs-file-thumb "foo0.jpg" "testsite"))))

; now multiplex.util
(deftest test-is-custom-host-1
  (is (= "example.f5n.de" (is-custom-host "example.f5n.de")))
  (is (= false (is-custom-host (:site-url (config/env :multiplex))))))

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
  (is (= ""    (file-extension "")))
  (is (= ""    (file-extension "foo")))
  (is (= ""    (file-extension ".foo")))
  (is (= "c"   (file-extension "test.c")))
  (is (= "m3u" (file-extension "test.foo.m3u")))
  (is (= "jpg" (file-extension "/test.jpg")))
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

(deftest test-video-info-vimeo-1
  (let [info (video-info "https://vimeo.com/62518619")]
    (is (= "vimeo" (:site info)))
    (is (= "62518619" (:code info)))))

(deftest test-video-info-mixcloud-1
  (let [info (video-info "https://www.mixcloud.com/ZorrinooohH/februar-2019-retrospective/")]
    (is (= "mixcloud" (:site info)))
    (is (= "%2FZorrinooohH%2Ffebruar-2019-retrospective%2F" (:code info)))))

(deftest test-video-info-soundcloud-1
  (let [info (video-info "https://soundcloud.com/zuckerton-records/klangkuenstler-munich-motion")]
    (is (= "soundcloud" (:site info)))
    (is (= "%2Fzuckerton-records%2Fklangkuenstler-munich-motion" (:code info)))))

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

(deftest test-add-fields-1
  (let [input {:url "http://www.youtube.com/watch?v=asdf" :meta ""}
        result (add-fields input)]
    (is (= "http://static.mpx1.f5n.de/dump-test" (:thumb-path result)))
    (is (= nil (:thumbnail result)))
    (is (= {} (:meta result)))
    (is (= nil (:tags result))))
  (let [input {:url "http://www.youtube.com/watch?v=asdf"
               :meta "{\"code\":\"foo\",\"site\":\"bar\"}"
               :tags "asdf"}
        result (add-fields input)]
    (is (= "foo" (:code result)))
    (is (= "bar" (:site result)))
    (is (= "asdf" (:tags result)))))

(deftest test-fix-time-fields-1
  (let [crea1   (java.time.LocalDateTime/of 2022 1 2 11 22 33)
        exp-s1  "2022-01-02 11:22"
        exp-ts1 "2022-01-02T11:22:33Z"]
    (is (= [exp-s1 exp-ts1] (convert-time crea1)))
    (is (= {:created exp-s1 :created-ts exp-ts1
            :updated exp-s1 :updated-ts exp-ts1}
           (fix-time-fields {:created crea1 :updated crea1})))))

(deftest test-fix-url-field-1
  (is (= {:url ""} (fix-url-field {})))
  (is (= {:url ""} (fix-url-field {:url ""})))
  (is (= {:url "http://static.mpx1.f5n.de/dump-test/foo.png"} (fix-url-field {:url "/dump-test/foo.png"})))
  (is (= {:url "https://example.org/foo.png"} (fix-url-field {:url "https://example.org/foo.png"}))))

(deftest test-set-author-1
  (let [input {}
        result (set-author input)]
    (is (= {:author {:uid nil :url "http://:3030"}} result)))
  (let [input {:author 23 :uid 24 :hostname "asd1.mpx1.f5n.de"}
        result (set-author input)]
    (is (= {:author {:uid 23 :hostname "asd1.mpx1.f5n.de"
                     :url "http://asd1.mpx1.f5n.de:3030"}} result)))
  (let [input {:author 23 :hostname "asd1.mpx1.f5n.de"
               :foo 42 :bar "asd"
               :title "xTitle" :avatar "a.png" :theme "t3" :is_private true}
        result (set-author input)]
    (is (= {:foo 42 :bar "asd"
            :author {:uid 23 :hostname "asd1.mpx1.f5n.de"
                     :title "xTitle" :avatar "a.png" :theme "t3" :is_private true
                     :url "http://asd1.mpx1.f5n.de:3030"}} result))))

(deftest test-keywordize-1
  (is (= {} (keywordize {})))
  (is (= {:foo1 "bar1"} (keywordize {"foo1" "bar1"})))
  (is (= {:foo1 "bar1" :foo2 "bar2"} (keywordize {"foo1" "bar1" "foo2" "bar2"}))))

(deftest test-calculate-pagination-1
  (is (= {:page-newer nil :page 1 :page-older nil
          :pages [1] :limit 10
          :page-count 1 :post-count 9} (calculate-pagination 10 1 9)))
  (is (= {:page-newer nil :page 1 :page-older 2
          :pages [1 2] :limit 10
          :page-count 2 :post-count 20} (calculate-pagination 10 1 20)))
  (is (= {:page-newer 1 :page 2 :page-older nil
          :pages [1 2] :limit 10
          :page-count 2 :post-count 20} (calculate-pagination 10 2 20)))
  (is (= {:page-newer 2 :page 3 :page-older 4
          :pages [1 2 3 4 5] :limit 5
          :page-count 5 :post-count 21} (calculate-pagination 5 3 21)))
)

(deftest test-type-pagination-1
  (is (= "?type=video" (type-pagination "video" 0 10)))
  (is (= "?type=video" (type-pagination "video" 1 10)))
  (is (= "?type=video&page=2" (type-pagination "video" 2 10)))
  (is (= "?type=video&limit=11" (type-pagination "video" 1 11)))
  (is (= "?type=video&limit=11&page=2" (type-pagination "video" 2 11))))

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

(deftest test-hash-filename-1
  (is (= "e226e44d039e18ebc5e3bc10311e96d0fe9bc737" (hash-filename "/tmp/foo.html"))))

(deftest test-join-params-1
  (is (= "" (join-params {})))
  (is (= "&a=b" (join-params {:a "b"})))
  (is (= "&a=b%20c" (join-params {:a "b c"})))
  (is (= "&a=b&c=3" (join-params {:a "b" :c 3}))))

(deftest test-unwrap-tags-1
  (is (= {:tags ""} (unwrap-tags {})))
  (is (= {:tags ""} (unwrap-tags {:tags nil})))
  (is (= {:tags ""} (unwrap-tags {:tags []})))
  (is (= {:tags "foo"} (unwrap-tags {:tags ["foo"]})))
  (is (= {:tags "foo,bar"} (unwrap-tags {:tags ["foo", "bar"]}))))

(deftest test-sanitize-tags-1
  (is (= [] (sanitize-tags "")))
  (is (= ["foo"] (sanitize-tags "foo")))
  (is (= ["foo","bar"] (sanitize-tags "foo,bar")))
  (is (= ["foo","bar"] (sanitize-tags "Foo,BAR")))
  (is (= ["_fx","b12"] (sanitize-tags "_fx,B12")))
  (is (= ["fo_","b-r"] (sanitize-tags "fo_,b-r,fa;il"))))

(deftest test-get-filename-1
  (is (= "" (get-filename "")))
  (is (= "" (get-filename "http://example.org")))
  (is (= "" (get-filename "http://example.org/")))
  (is (= "foo.jpg" (get-filename "http://example.org/foo.jpg")))
  (is (= "foo.jpg" (get-filename "http://example.org/bar/foo.jpg"))))

(deftest test-audit-1
  (is (= ["event.foo" 3] (audit :foo 3)))
  (is (= ["event.foo.bar" {:x 3}] (audit :foo-bar {:x 3}))))
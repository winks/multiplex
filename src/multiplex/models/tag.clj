(ns multiplex.models.tag
  (:require [korma.core :refer :all]
            [multiplex.config :as config]
            [multiplex.models.db :as db]
            [multiplex.util :as util]))

; prepare
(defn prepare-map []
  {:uid nil
   :username nil
   :email nil
   :password nil
   :apikey nil
   :signupcode nil
   :created nil
   :updated nil})

(defn sanitize-tags [s]
  (let [tags (clojure.string/split s #" ")]
    (clojure.string/join "," (remove empty? (map clojure.string/trim tags)))))

; SQLish

; abstraction

(ns multiplex.models.tag
  (:use korma.core
        [korma.db :only (defdb mysql postgres)])
  (:require [multiplex.config :as config]
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
    (clojure.string/join "," (filter seq (map clojure.string/trim tags)))))

; SQLish

; abstraction


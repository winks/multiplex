(ns multiplex.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[multiplex started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[multiplex has shut down successfully]=-"))
   :middleware identity})

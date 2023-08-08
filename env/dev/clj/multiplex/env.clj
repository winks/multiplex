(ns multiplex.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [multiplex.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[multiplex started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[multiplex has shut down successfully]=-"))
   :middleware wrap-dev})

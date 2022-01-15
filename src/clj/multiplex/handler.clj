(ns multiplex.handler
  (:require
    [mount.core :as mount]
    [multiplex.env :refer [defaults]]
    [multiplex.layout :refer [error-page]]
    [multiplex.middleware :as middleware]
    [multiplex.routes.home :refer [home-routes]]
    [multiplex.routes.services :refer [service-routes]]
    [reitit.ring :as ring]
    [reitit.swagger-ui :as swagger-ui]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(home-routes)])
       ;(service-routes)])
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path   "/swagger-ui"
         :url    "/api/swagger.json"
         :config {:validator-url nil}})
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type
        (wrap-webjars (constantly nil)))
      (ring/create-default-handler
        {:not-found
         (constantly (error-page {:status 404, :title "404 - Page not found"}))
         :method-not-allowed
         (constantly (error-page {:status 405, :title "405 - Not allowed"}))
         :not-acceptable
         (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))

(defn app []
  (middleware/wrap-base #'app-routes))

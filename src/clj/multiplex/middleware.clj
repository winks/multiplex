(ns multiplex.middleware
  (:require
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.accessrules :refer [restrict]]
    [buddy.auth.backends.session :refer [session-backend]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [clojure.tools.logging :as log]
    [multiplex.config :refer [env]]
    [multiplex.env :refer [defaults]]
    [multiplex.layout :refer [error-page]]
    [multiplex.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.middleware.session.cookie :refer [cookie-store]]
    [ring.middleware.ssl :refer [wrap-ssl-redirect wrap-forwarded-scheme]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def cookie-bytes (byte-array (map byte [62 5 83 101 25 58 115 55 5 23 89 89 46 110 29 13])))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn on-error [request response]
  (error-page
    {:status 403
     :title (str "Access to " (:uri request) " is not authorized")}))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-error}))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-forwarded-scheme
      ;wrap-ssl-redirect
      wrap-auth
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true :max-age 86400}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :store] (cookie-store {:key cookie-bytes}))
            (assoc-in [:session :cookie-name] "multiplex-sessions")))
      wrap-internal-error))

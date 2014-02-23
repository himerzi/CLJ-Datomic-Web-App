(ns dactic-server.core
    (:require [liberator.core :refer [resource defresource]]
              [ring.middleware.params :refer [wrap-params]]
              [ring.adapter.jetty :refer [run-jetty]]
              [monger.core :as mg]
              [ring.util.response :refer [resource-response redirect]]
              [ring.util.request :refer [request-url]]
              
              [compojure.core :refer [defroutes GET POST ANY DELETE]]
              [compojure.handler :as compojure-handler]
              [compojure.route :refer [resources]]
              ;;general middleware
              [ring.middleware.json]
              ;;Controlers
              [dactic-server.controllers.auth :as auth-ctrl]
              [dactic-server.controllers.course]
              ;;DB
              [dactic-server.models.util :as db-util]
              [dactic-server.models.db-fn :as db-fn]
              ;;sessions
              [noir.session :as session]
              [noir.util.middleware :refer [app-handler]]
              [ring.middleware.session.cookie :refer [cookie-store]]
              ;; debugging
              [liberator.dev :refer [wrap-trace]]
              [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
              [ring.middleware.reload :refer [wrap-reload]]))




;; should be in utils or something
(defroutes app-routes
  (GET "/" [] (resource-response "index.html" {:root ""}))
  (POST "/login" { {email :email password :password name :name} :body}
        (fn [_] (dactic-server.controllers.auth/login-resource email password)))
  (DELETE "/logout" [] (fn [_] ( dactic-server.controllers.auth/logout-resource)))
  (POST "/register" { {email :email password :password name :name} :body} (fn [_]
                                            (auth-ctrl/registration-resource name email password)
                                            ))
  (GET "/user" [] (fn [_] (dactic-server.controllers.auth/user-resource)))
  (GET "/users/:slug" [slug] (fn [_] (dactic-server.controllers.auth/user-detail-resource slug)))
  ;;(POST "/courses/:id/charge" {:keys [body]} (dactic-server.controllers.charge/charge-resource body))
  (POST "/courses/:id/resources" {:keys [body]} (dactic-server.controllers.course/course-links-resource body))
  (DELETE "/courses/:id/resources" [eid cid] (dactic-server.controllers.course/course-links-resource {:eid eid :cid cid}))
  (POST "/courses" {:keys [body]} (fn [_]  (dactic-server.controllers.course/course-resource body)))
  (GET "/courses" {:keys [body]} (fn [_]  (dactic-server.controllers.course/course-list-resource)))
  (ANY "/courses/:id" {body :body {id :id} :params } (fn [_] (dactic-server.controllers.course/course-entry-resource id body)))
  
  (resources "/" {:root ""}))
 ;; compojure.core/routes

(defn force-https [handler]
  (fn [{:as request}]
    (let [{{protocol "x-forwarded-proto"} :headers} request]
      ;;heroku will convert https to http before it reaches us, and
      ;;indicate it has done so by placing this header
      (cond
       (=  protocol "https") (handler request)
       (nil? protocol) (handler request)
       :else  (redirect (clojure.string/replace-first
                                           (request-url request)
                                           #"http://" "https://"))))))

(def app-middleware
  (-> (app-handler [app-routes] :session-options {:store (cookie-store)})
      (ring.middleware.json/wrap-json-body {:keywords? true})
      compojure-handler/api 
      wrap-stacktrace-log
      wrap-reload
      force-https))

(defn start [port]
  (run-jetty #'app-middleware {:port port :join? false}))

(defn -main []
  (let [port (Integer/parseInt
              (or (System/getenv "PORT") "8080"))]
    (db-util/-init)
    (.println System/out "initializing stuff")
    (dactic-server.models.db-fn/-init)
    (start port)))




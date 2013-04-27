(ns sse-chat.service
    (:require [io.pedestal.service.http :as bootstrap]
              [io.pedestal.service.http.route :as route]
              [io.pedestal.service.http.body-params :as body-params]
              [io.pedestal.service.http.route.definition :refer [defroutes]]
              [io.pedestal.service.interceptor :refer [defon-response]]
              [io.pedestal.service.http.sse :as sse]
              [io.pedestal.service.log :as log]
              [clojure.java.io :as io]
              [comb.template :as comb]
              [ring.util.response :as ring-resp]))

;;; helpers
(defn- render-erb
  "Renders an erb file with optional bindings."
  ([template] (render-erb template {}))
  ([template template-bindings]
     (comb/eval (slurp (io/resource template)) template-bindings)))

(defn- erb
  "Given an erb file's basename, renders it wrapped in a default layout"
  [& args]
  (let [basename (format "public/%s.erb" (name (first args)))]
    (ring-resp/response (render-erb "public/layout.erb"
                                    {:yield (apply render-erb (cons basename (rest args)))}))))

;;; endpoints and sse setup
(defn home-page
  [request]
  (->
   (if-let [user (-> request :query-params :user)]
     (erb :chat {:user user})
     (erb :login))
   (ring-resp/content-type "text/html")))

(def subscribers (atom []))

(defn add-subscriber [context]
  (swap! subscribers conj context))

(defn remove-subscriber [context]
  (log/info :msg "Removing nonexistent user")
  (swap! subscribers #(remove #{context} %))
  ;; should be removed but fails unexpectedly
  #_(sse/end-event-stream context))

(defn publish
  [request]
  (doseq [sse-context @subscribers]
    (try
      (sse/send-event sse-context "message" (-> request :form-params (get "msg")))
      (catch java.io.IOException e
        (remove-subscriber sse-context))))
  {:status 204})

(defroutes routes
  [[["/" {:get home-page :post publish}
     ^:interceptors [(body-params/body-params)]
     ["/stream" {:get [::stream (sse/sse-setup add-subscriber)]}]]]])

;; You can use this fn or a per-request fn via io.pedestal.service.http.route/url-for
(def url-for (route/url-for-routes routes))

;; Consumed by sse-chat.server/create-server
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; :bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::boostrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty or :tomcat (see comments in project.clj
              ;; to enable Tomcat)
              ;;::bootstrap/host "localhost"
              ::bootstrap/type :jetty
              ::bootstrap/port (Integer. (or (System/getenv "PORT") 8080))})

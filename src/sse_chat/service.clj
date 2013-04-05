(ns sse-chat.service
    (:require [io.pedestal.service.http :as bootstrap]
              [io.pedestal.service.http.route :as route]
              [io.pedestal.service.http.body-params :as body-params]
              [io.pedestal.service.http.route.definition :refer [defroutes]]
              [io.pedestal.service.interceptor :refer [defon-response]]
              [io.pedestal.service.http.sse :as sse]
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

(defon-response html-content-type
  [response]
  (ring-resp/content-type response "text/html"))

;;; endpoints and sse setup
(defn home-page
  [request]
  (if-let [user (-> request :query-params :user)]
    (erb :chat {:user user})
    (erb :login)))

(def subscribers (atom []))

(defn add-subscriber [context]
  (swap! subscribers conj context))

#_(defn send-counter
  "Counts down to 0, sending value of counter to sse context and
  recursing on a different thread; ends event stream when counter
  is 0."
  [ctx count]
  (sse/send-event ctx "count" (str count ", thread: " (.getId (Thread/currentThread))))
  (Thread/sleep 2000)
  (if (> count 0)
    (future (send-counter ctx (dec count)))
    (sse/end-event-stream ctx)))

(defn sse-stream-ready
  "Starts sending counter events to client."
  [ctx]
  (add-subscriber ctx))

(defn publish
  [request]
  #_(prn "POST:" (-> request :params (get "msg")) (count @subscribers) @subscribers)
  (doseq [sse-context @subscribers]
    (sse/send-event sse-context "msg" (-> request :params (get "msg"))))
  {:status 204})

(defroutes routes
  [[["/" {:get home-page :post publish}
     ;; Set default interceptors for /about and any other paths under /
     ^:interceptors [(body-params/body-params) html-content-type]
     ["/stream" {:get [::stream (sse/sse-setup sse-stream-ready)]}]]]])

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

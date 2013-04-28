(ns sse-chat.service-test
  (:require [clojure.test :refer :all]
            [io.pedestal.service.test :refer :all]
            [io.pedestal.service.http :as bootstrap]
            [sse-chat.service :as service]))

(def service
  (::bootstrap/service-fn (bootstrap/create-servlet service/service)))

(deftest post-to-publish
  (is (=
       (:status (response-for service :post "/"))
       204)))

(deftest home-page-test
  (testing "initially asks for user name"
    (is (.contains
         (:body (response-for service :get "/"))
         "User Name:")))
  ;; TODO: fix regex bug
  #_(testing "with name prompts you to chat"
    (is (.contains
         (:body (response-for service :get "/?user=me"))
         "type message here.."))))

;;; TODO: service.test not possible yet...
;;; Error: java.lang.AbstractMethodError: io.pedestal.service.test$test_servlet_request$reify__266.setAttribute(Ljava/lang/String;Ljava/lang/Object;)V
#_(deftest stream-adds-a-subscriber
  (response-for service :get "/stream")
  (is (= (count @service/subscribers)
         1)))
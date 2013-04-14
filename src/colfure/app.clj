(ns colfure.app
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:import org.codehaus.jackson.JsonParseException)
  (:require [clj-json.core :as json]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defroutes handler
           (POST "/recommendations" [user item]
                (json-response {"user" user "item" item} 201))
           (GET "/recommendations" []
                (json-response {"items" []}))
           (GET "/health_check" []
                (json-response {"status" "running"})))

(def error-codes
  {:invalid 400
   :not-found 404})

(defn wrap-error-handling [handler]
  (fn [req]
    (try
      (or (handler req)
          (json-response {"error" "resource not found"} (:not-found error-codes)))
      (catch JsonParseException e
                (json-response {"error" "malformed json"} (:invalid error-codes))))))

(def run
  (-> handler
    wrap-json-params
    wrap-error-handling))

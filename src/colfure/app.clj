(ns colfure.app
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:import org.codehaus.jackson.JsonParseException)
  (:require [colfure.vec :as vec-utils]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [somnium.congomongo :as mongo]
            [clj-json.core :as json]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn item-vector
  "Return a item-based vector given a item node"
  [node]
  (def vec-size (mongo/fetch-count :users))
  (loop [users (nn/all-connected-out (:id node) :types [:visited-by])
         res (vec (replicate vec-size 0))]
    (if (empty? users)
      res
      (let [user (first users)
            rel (nrl/first-outgoing-between node user [:visited-by])
            pos (:seq (mongo/fetch-one :users :where {:node (:id user)}))]
        (recur (rest users) (assoc res pos (:views (:data rel))))))))

(defn create-event
  "Creates a relationship between item and user"
  [user item]
  (let [neo-user (nn/create-unique-in-index "users_idx" "users" user {})
        neo-item (nn/create-unique-in-index "items_idx" "items" item {})
        cur-rel (nrl/first-outgoing-between neo-item neo-user [:visited-by])]

    (if (nil? (mongo/fetch-one :users :where {:node (:id neo-user)}))
      (mongo/insert! :users {:node (:id neo-user) :seq (mongo/fetch-count :users)}))

    (if (nil? (mongo/fetch-one :items :where {:node (:id neo-item)}))
      (mongo/insert! :items {:node (:id neo-item)}))

    (def views-count
      (if (nil? cur-rel)
        (:views (:data (nrl/create neo-item neo-user :visited-by {:views 1})))
        (:views (nrl/update cur-rel {:views (+ 1 (:views (:data cur-rel)))}))))

    {"user" (:id neo-user) "item" (:id neo-item) "views" views-count}))

(defn process-similarities
  []
  (for [item (mongo/fetch :items)]
    (let [neo-item (nn/get (:node item))
          item-vec (item-vector neo-item)]
      (for [other (mongo/fetch :items :where {:node {:$ne (:node item)}})]
        (let [neo-other (nn/get (:node other))
              other-vec (item-vector neo-other)]
          (def cosine (vec-utils/cos item-vec other-vec))
          (def rel-key (str "s" (:id neo-item) "e" (:id neo-other)))
          (nrl/create-unique-in-index neo-item neo-other
                                      :similarity :sims_idx :similarities
                                      rel-key {:cosine cosine})nil))
      nil)))

(defroutes handler
           (POST "/events"
                 [user item]
                 (json-response (create-event user item) 201))
           (GET "/recommendations"
                []
                (json-response {"items" []}))
           (GET "/health_check"
                []
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

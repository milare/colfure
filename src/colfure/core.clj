(ns colfure.core
  (:gen-class)
  (:use ring.adapter.jetty)
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:import org.codehaus.jackson.JsonParseException)
  (:require [colfure.vec :as cv]
            [clj-json.core :as json]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]))

; Enviroment setup
(defn set-env []
  (nr/connect! "http://localhost:7474/db/data/"))


(defn most-similar-to
  [item n]
  (def nodes [])
  (doseq [sim-rel
          (take n
                (sort #(> (:cosine (:data %1)) (:cosine (:data %2)))
                      (nrl/all-for item :types [:similarities])))]
      (let [node
            (if (= (:n1 (:data sim-rel)) (:id item))
              (nn/get (:n2 (:data sim-rel)))
              (nn/get (:n1 (:data sim-rel))))]
        (def nodes (conj nodes node))))
  nodes)

(defn item-vector
  "Return a item-based vector given a item node"
  [node users-count]
  (loop [users (nn/all-connected-out (:id node) :types [:events])
         res (vec (replicate users-count 0))]
    (if (empty? users)
      res
      (let [user (first users)
            rel (nrl/first-outgoing-between node user [:events])
            pos (:idx (:data user))]
        (recur (rest users) (assoc res pos (:views (:data rel))))))))

(defn process-similarities
  "Process the similarities between item nodes"
  []
  (def users-count (count (nn/traverse 0 :relationships [{:direction "out" :type "users"}])))
  (doseq [node (nn/traverse 0 :relationships [{:direction "out" :type "similarities"}])]
    (doseq [conn-node (nn/all-connected-out (:id node) :types [:similarities])]
      (let [item-vec (item-vector node users-count)
            other-vec (item-vector conn-node users-count)
            rel (nrl/first-outgoing-between node conn-node [:similarities])
            cosine (cv/cos item-vec other-vec)]
        (println (:id node) "<->" (:id conn-node) " - similarity: " cosine)
        (nrl/update rel {:cosine cosine :n1 (:id node) :n2 (:id conn-node)})))))

(defn start-similarities
  "Creates empty relationships of similarity between an item and every other item"
  [item]
  (future
    (doseq [node (nn/traverse 0 :relationships [{:direction "out" :type "similarities"}])]
      (nrl/create node item :similarities {}))) item)

(defn find-or-create-item
  "Find or create an item node with all relationships"
  [item-val]
  (let [item (try (nn/find-one "items-by-key" "key" item-val) (catch Exception e nil))]
    (if (nil? item)
      (start-similarities (nn/create-unique-in-index "items-by-key" "key" item-val {:key item-val}))
      item)))

(defn find-or-create-user
  "Find or create an user node with its relationships"
  [user-val]
  (let [user (try (nn/find-one "users-by-key" "key" user-val) (catch Exception e nil))]
    (if (nil? user)
      (let [last-user (last (nn/traverse 0 :relationships [{:direction "out" :type "users"}]))
            idx (if (= (nn/get 0) last-user) 0 (inc (:idx (:data last-user))))
            new-user (nn/create-unique-in-index "users-by-key" "key" user-val {:key user-val :idx idx})]
        (nrl/create 0 new-user :users {}) new-user)
      user)))

(defn inc-views-count
  "Increase item views count for an user and return the views count"
  [user item]
  (let [rel (nrl/first-outgoing-between item user [:events])]
    (if (nil? rel)
      (:views (:data (nrl/create item user :events {:views 1})))
      (:views (nrl/update rel {:views (inc (:views (:data rel)))})))))

(defn create-event
  "Creates a item node and a user node and set them up"
  [user-val item-val]
  (let [user  (find-or-create-user user-val)
        item  (find-or-create-item item-val)
        views (inc-views-count user item)]
    {:user (:id user) :item (:id item) :views views}))

; JSON handling
(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

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

; Routes
(defroutes handler
           (POST "/events"
                 [user item]
                 (json-response (create-event user item) 201))
           (GET "/recommendations/:item/:n"
                [item n]
                (let [item-node (try (nn/find-one "items-by-key" "key" item) (catch Exception e nil))]
                  (json-response {"items" (map :data (most-similar-to item-node (Integer. (re-find  #"\d+" n )))) })))
           (GET "/health_check"
                []
                (json-response {"status" "running"})))


; Main
(def run
  (-> handler
    wrap-json-params
    wrap-error-handling))

(defn -main []
  (set-env)
  (run-jetty #'run {:port 8080})
)

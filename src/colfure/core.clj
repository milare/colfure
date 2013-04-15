(ns colfure.core
  (:gen-class)
  (:use ring.adapter.jetty)
  (:require colfure.vec
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]
            [somnium.congomongo :as mongo]
            [colfure.app :as app]))

(defn set-env []
  (nr/connect! "http://localhost:7474/db/data/")
  (mongo/set-connection! (mongo/make-connection "colfure" :host "127.0.0.1" :port 27017))
  (mongo/add-index! :users [:node] :unique true)
  (mongo/add-index! :items [:node] :unique true)
  nil)

(defn -main []
  (set-env)
  (run-jetty #'app/run {:port 8080})
)

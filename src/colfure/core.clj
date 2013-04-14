(ns colfure.core
  (:gen-class)
  (:use ring.adapter.jetty)
  (:require colfure.vec
            [colfure.app :as app]))

(defn -main []
  (run-jetty #'app/run {:port 8080})
)

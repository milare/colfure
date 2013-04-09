(ns colfure.core
  (:gen-class)
  (:use ring.adapter.jetty)
  (:require colfure.vec colfure.web))

(defn -main []
  (run-jetty #'colfure.web/app {:port 8080})
)

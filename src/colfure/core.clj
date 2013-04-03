(ns colfure.core
  (:require colfure.vec))

(defn -main []
  (def v1 [1 2 1])
  (def v2 [1 1 1])
  (def sim (colfure.vec/cos v1 v2))
  (println "The similarity btw" v1 " and " v2 " is " sim)
)

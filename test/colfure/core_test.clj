(ns colfure.core-test
  (:use clojure.test
        colfure.core))

(deftest test-item-seq
  (is (= '(0 0) (item-seq [[1 2 3] [2]] 0)))
  (is (= '(1 1 0) (item-seq [[1 2 3] [2] [3 4 5]] 2)))
  (is (= '(1 1 0 1 1 0) (item-seq [[1 2 3] [2 3] [0 4 5] [3] [1 3] [4]] 3))))


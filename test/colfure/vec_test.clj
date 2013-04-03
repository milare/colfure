(ns colfure.vec-test
  (:use clojure.test
        colfure.vec))

(deftest test-dot
  (is (= 0 (dot [1 1 1] [-1 1 0])))
  (is (= 2 (dot [1 2 3] [3 -2 1]))))

(deftest test-norm
  (is (= 0.0 (norm [0 0])))
  (is (= (Math/sqrt 3) (norm [1 1 1])))
  (is (= (Math/sqrt 6) (norm [-1 2 1]))))

(deftest test-cos
  (is (= 0.0 (cos [0 0 0] [2 3 4])))
  (is (= 0.0 (cos [1 2 3] [0 0 0])))
  (is (= (/ 1 (* (Math/sqrt 3) 1) (cos [1 1 1] [0 1 0]))))
  (is (= (/ 2 (* (Math/sqrt 6) (Math/sqrt 3))) (cos [-1 2 1] [1 1 1]))))

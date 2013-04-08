(ns colfure.vec)

(defn dot
  "Dot product of vectors v1 and v2"
  [v1 v2] (reduce + (map * v1 v2)))

(defn norm
  "Euclidean norm of a vector v"
  [v] (Math/sqrt (dot v v)))

(defn cos
  "Cosine between two vectors v1 and v2"
  [v1 v2]
  (try (/ (dot v1 v2) (* (norm v1) (norm v2))) (catch ArithmeticException e 0.0)))

(defn parametrize
  "Parametrizes vector v to a unity vector with size s"
  [v s]
  (loop [c 0 cc 0 res []]
    (if (= c s)
      res
      (recur (inc c)
        (if (= (nth v cc 0) c) (inc cc) cc)
        (conj res (if (= (nth v cc -1) c) 1 0))))))


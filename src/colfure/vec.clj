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

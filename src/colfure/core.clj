(ns colfure.core
  (:require colfure.vec))

(defn item-seqs
  "Returns the items sequences based on vector of vectors vs and max-items s"
  [vs s]
  (loop [res [] c 0]
    (if (= c s)
      res
      (recur (conj res
                   (with-meta (map #(nth % c) (vec (map #(colfure.vec/parametrize % s) vs))) {:item c}))
             (inc c)))))

(defn sim-for
  "Returns the most similar items to an item i"
  [items i]
  (def cur (nth (vec items) i))
  (sort #(> (:sim %1) (:sim %2))
    (for [x items :when (not= (:item (meta x)) i)]
      {:sim (colfure.vec/cos x cur) :item (:item (meta x))} )))

(defn -main []
  (def max-items 10)
  (def users-visits {:x [1 3 5 6] :y [1 2 3] :z [1 3 4 5]})
  (def items (item-seqs (vals users-visits) max-items))
  (prn (take 5 (sim-for items 1)))
)

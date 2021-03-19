(ns cq.core)

(defn- thread-last
  [x exps]
  (eval (concat `(->> ~x) exps)))

(defn query
  [data exps]
  (thread-last data exps))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))


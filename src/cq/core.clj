(ns cq.core)

(declare ^:dynamic .)

(defn with-dot
  [x form]
  (binding [. x
            *ns* (the-ns 'cq.core)]
    (eval form)))

(defn |*
  [form]
  `((fn [x#]
      (with-dot x#
        (quote
         ~form)))))

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

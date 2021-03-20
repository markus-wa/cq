(ns cq.core
  (:require [sci.core :as sci]))

(declare ^:dynamic .)

(defn- eval-form
  [form]
  (sci/eval-form (sci/init {}) form))

(defn with-dot
  [x form]
  (binding [. x
            *ns* (the-ns 'cq.core)]
    (eval-form form)))

(defn |*
  [form]
  `((fn [x#]
      (with-dot x#
        (quote
         ~form)))))

(defn- thread-last
  [x exps]
  (eval-form (concat `(->> ~x) exps)))

(defn query
  [data exps]
  (thread-last data exps))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

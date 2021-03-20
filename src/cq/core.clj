(ns cq.core
  (:require [sci.core :as sci]))

(defn- eval-form
  [opts form]
  (sci/eval-form (sci/init opts) form))

(defn |*
  [form]
  `((fn [x#]
      (~'eval-f {:bindings {'. x#}}
                '~form))))

(defn- thread-last
  [x exps]
  (eval-form {:bindings {'eval-f eval-form}}
             (concat `(->> ~x) exps)))

(defn query
  [data exps]
  (thread-last data exps))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

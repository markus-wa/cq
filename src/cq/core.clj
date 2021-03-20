(ns cq.core
  (:require [sci.core :as sci]
            [com.rpl.specter :as spc]))

(def bindings
  {'select spc/select*
   'ALL spc/ALL})

(defn- my-eval
  [opts form]
  (let [ctx (-> opts
                (update :bindings #(merge % bindings))
                sci/init)]
    (sci/eval-form ctx form)))

(defn |*
  [form]
  `((fn [x#]
      (~'my-eval {:bindings {'. x#}}
                '~form))))

(defn- thread-last
  [x exps]
  (my-eval {:bindings {'my-eval my-eval}}
           (concat `(->> ~x) exps)))

(defn query
  [data exps]
  (thread-last data exps))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

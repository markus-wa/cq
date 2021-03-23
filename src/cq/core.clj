(ns cq.core
  (:require [sci.core :as sci]))

;; set up sci bindings for specter
(require 'com.rpl.specter)

(def specter-bindings
  (let [specter-publics (ns-publics (the-ns 'com.rpl.specter))
        ->bindings
        (fn [acc [k v]]
          (let [k* (symbol (format "%s*" k))
                v (or (specter-publics k*) v)]
            (assoc acc k (var-get v))))]
    (reduce ->bindings {} specter-publics)))

(def bindings specter-bindings)

(defn- my-eval
  [op data exps]
  (let [data-var `x#
        opts {:bindings {'my-eval my-eval
                         data-var data}}
        form (concat `(~op ~data-var) exps)]
    (sci/eval-form (sci/init opts)
                   `(do
                      (~'require '[clojure.string :as ~'str])
                      ~form))))

(def ->* (partial my-eval '->))
(def ->>* (partial my-eval '->>))

(defn- query*
  [data [[e1] :as exps]]
  (if (seq exps)
    (cond
      (= e1 '->)
      (query* (->* data (second exps)) (drop 2 exps))
      (= e1 '->>)
      (query* (->>* data (second exps)) (drop 2 exps))
      :else
      (query* (->>* data (first exps)) (rest exps)))
    data))

(def special?
  #{'->
    '->>})

(defn query
  [data exps]
  (query* data (partition-by special? exps)))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

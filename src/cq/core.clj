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
(def some->* (partial my-eval 'some->))
(def some->>* (partial my-eval 'some->>))

(def ->threading-fn
  {'-> ->*
   '->> ->>*
   'some-> some->*
   'some->> some->>*})

(defn- query*
  [data [[e1] next-exp & future-exps :as exps]]
  (if (seq exps)
    (if-let [thread-fn (->threading-fn e1)]
      (query* (thread-fn data next-exp) future-exps)
      (query* (->>* data e1) (rest exps)))
    data))

(defn query
  [data exps]
  (query* data (partition-by ->threading-fn exps)))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

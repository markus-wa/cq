(ns cq.core
  (:require [sci.core :as sci]
            [clojure.walk :refer [postwalk]]))

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
  [opts form]
  (let [ctx (-> opts
                (update :bindings #(merge % bindings {'my-eval my-eval}))
                sci/init)]
    (sci/eval-form ctx form)))

(defn dotfn
  [form]
  ;; Clojure doesn't like . as var name
  (let [replace-dot #(if (= % '.) 'dot %)
        form (postwalk replace-dot form)]
    `(fn [~'dot]
       ~form)))

(defn |*
  [form]
  `(~(dotfn form)))

(defn map*
  [form]
  `(map ~(dotfn form)))

(defn- thread-last
  [x exps]
  (my-eval nil (concat `(->> '~x) exps)))

(defn query
  [data exps]
  (thread-last data exps))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

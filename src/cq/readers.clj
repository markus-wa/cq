(ns cq.readers
  (:require [clojure.walk :refer [postwalk]]))

(defn dotfn
  [form]
  ;; Clojure doesn't like . as var name
  (let [dot `dot#
        replace-dot #(if (= % '.) dot %)
        form (postwalk replace-dot form)]
    `(fn [~dot]
       ~form)))

(defn |*
  [form]
  `(~(dotfn form)))

(defn map*
  [form]
  `(map ~(dotfn form)))

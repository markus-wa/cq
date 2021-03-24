(ns cq.core
  (:require [sci.core :as sci]
            [medley.core :as m]))

;; set up sci bindings for specter and medley
(require 'com.rpl.specter)

(def specter-bindings
  (let [publics (ns-publics (the-ns 'com.rpl.specter))]
    (m/map-kv-vals
     (fn [k v]
       (let [k* (symbol (format "%s*" k))]
         (var-get (or (publics k*) v))))
     publics)))

(def medley-bindings
  (->> (the-ns 'medley.core)
       ns-publics
       (m/map-vals var-get)))

(def bindings
  (merge specter-bindings
         medley-bindings))

(defn- eval*
  [form opts]
  (sci/eval-form (sci/init opts) form))

(defn- eval-with-data
  [->form data]
  (let [data-var `x#
        bindings (merge bindings
                        {'eval-with-data eval-with-data
                         data-var        data})
        opts {:bindings bindings}]
    (eval* (->form data-var) opts)))

(defn- ->thread-fn
  [op exps]
  (fn [data-var]
    `(~@op ~data-var ~@exps)))

(def ->* (partial ->thread-fn '(->)))
(def ->>* (partial ->thread-fn '(->>)))
(def some->* (partial ->thread-fn '(some->)))
(def some->>* (partial ->thread-fn '(some->>)))
(defn as->* [[var & exps]]
  (fn [data-var]
    `(as-> ~data-var ~var ~@exps)))

(def ->form-fn
  {'-> ->*
   '->> ->>*
   'some-> some->*
   'some->> some->>*
   'as-> as->*})

(defn- query*
  [data [[e1 :as first-exps] next-exps & future-exps :as exps]]
  (if (seq exps)
    ;; TODO: extract function
    (if-let [form-fn (->form-fn e1)]
      (query* (eval-with-data (form-fn next-exps) data) future-exps)
      (query* (eval-with-data (->>* first-exps) data) (rest exps)))
    data))

(defn query
  [data exps]
  (query* data (partition-by ->form-fn exps)))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

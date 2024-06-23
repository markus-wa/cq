(ns cq.core
  (:require [sci.core :as sci]
            [medley.core :as m]))

;; set up sci bindings for specter, medley & camel-snake-kebab
(require 'com.rpl.specter)
(require 'camel-snake-kebab.core)
(require 'xml-in.core)
(require 'clojure.instant)
(require 'clojure.data.csv)
(require 'clojure.data.json)
(require 'clojure.data.xml)

(def specter-bindings
  (let [publics (ns-publics (the-ns 'com.rpl.specter))]
    (m/map-kv-vals
     (fn [k v]
       (let [k* (symbol (format "%s*" k))]
         (var-get (or (publics k*) v))))
     publics)))

(defn ns-publics-vars
  [ns]
  (->> (the-ns ns)
       ns-publics
       (m/map-vals var-get)))

(def sci-ns-specs
  {'xml-in.core
   {:alias 'xml-in}
   'camel-snake-kebab.core
   {:alias 'csk}
   'medley.core
   {:alias 'm}
   'com.rpl.specter
   {:alias 's
    :bindings-override specter-bindings}
   'clojure.data.csv
   {:alias 'csv}
   'clojure.data.json
   {:alias 'json}
   'clojure.data.xml
   {:alias 'xml}
   'clojure.instant
   {:alias 'inst}})

(defn ns-spec->sci-ns
  [ns spec]
  (if-let [bindings (:bindings-override spec)]
    [ns bindings]
    [ns (ns-publics-vars ns)]))

(def sci-namespaces
  (m/map-kv ns-spec->sci-ns sci-ns-specs))

(def sci-ns-aliases
  (m/map-kv (fn [k v] [(:alias v) k]) sci-ns-specs))

(defn- eval*
  [form opts]
  (sci/eval-form (sci/init opts) form))

(defn- eval-with-data
  [->form data]
  (let [data-var `x#
        opts {:bindings
              {'eval-with-data eval-with-data
               data-var        data}
              :namespaces sci-namespaces
              :aliases sci-ns-aliases}]
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
    (if-let [form-fn (->form-fn e1)]
      (recur (eval-with-data (form-fn next-exps) data) future-exps)
      (recur (eval-with-data (->>* first-exps) data) (rest exps)))
    data))

(defn query
  [data exps]
  (query* data (partition-by ->form-fn exps)))

(defn run
  [read! write! exps]
  (-> (read!)
      (query exps)
      (write!)))

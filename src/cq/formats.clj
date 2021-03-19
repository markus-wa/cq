(ns cq.formats
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.pprint :as ppt]))

(defn ->json-reader
  [{:keys [key-fn]
    :or   {key-fn keyword}}]
  (fn [x]
    (json/read x :key-fn key-fn)))

(defn ->json-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (binding [*out* out]
        (json/pprint x)))
    (fn [x out]
      (binding [*out* out]
        (json/write x out)
        (println)))))

(defn ->edn-reader
  [_]
  edn/read)

(defn ->edn-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (ppt/pprint x out))
    (fn [x out]
      (binding [*out* out]
        (pr x)))))

(def formats
  {"json" {:->reader ->json-reader
           :->writer ->json-writer}
   "edn"  {:->reader ->edn-reader
           :->writer ->edn-writer}})

(defn format->reader
  [format in opts]
  (let [{:keys [->reader]}
        (get formats format)]
    (partial (->reader opts) in)))

(defn format->writer
  [format out opts]
  (let [{:keys [->writer]}
        (get formats format)]
    #((->writer opts) % out)))

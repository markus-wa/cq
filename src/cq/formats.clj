(ns cq.formats
  (:require [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.pprint :as ppt]
            [clojure.java.io :as io]
            [msgpack.core :as mp]
            [msgpack.clojure-extensions]))

(defn ->json-reader
  [{:keys [key-fn]
    :or   {key-fn keyword}}]
  (fn [x]
    (json/read x :key-fn key-fn)))

(defn ->json-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (binding [*out* (java.io.PrintWriter. out)]
        (json/pprint x)))
    (fn [x out]
      (binding [*out* (java.io.PrintWriter. out)]
        (json/write x *out*)
        (println)))))

(defn ->edn-reader
  [_]
  edn/read)

(defn ->edn-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (ppt/pprint x (java.io.PrintWriter. out)))
    (fn [x out]
      (binding [*out* (java.io.PrintWriter. out)]
        (pr x)
        (println)))))

(defn ->msgpack-reader
  [_]
  mp/unpack)

(defn ->msgpack-writer
  [_]
  (fn [data out]
    (mp/pack-stream data out)))

(def formats
  {"json"    {:->reader ->json-reader
              :->writer ->json-writer}
   "edn"     {:->reader ->edn-reader
              :->writer ->edn-writer}
   "msgpack" {:->reader ->msgpack-reader
              :->writer ->msgpack-writer}})

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

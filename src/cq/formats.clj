(ns cq.formats
  (:require [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.pprint :as ppt]
            [clojure.java.io :as io]
            [msgpack.core :as mp]
            [msgpack.clojure-extensions])
  (:import [java.io PushbackReader]))

(defn ->json-reader
  [{:keys [key-fn]
    :or   {key-fn keyword}}]
  (fn [in]
    (json/read (io/reader in) :key-fn key-fn)))

(defn ->json-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (binding [*out* (io/writer out)]
        (json/pprint x)))
    (fn [x out]
      (binding [*out* (io/writer out)]
        (json/write x *out*)
        (println)))))

(defn ->edn-reader
  [_]
  (fn [in]
    (edn/read (PushbackReader. (io/reader in)))))

(defn ->edn-writer
  [{:keys [pretty]}]
  (if pretty
    (fn [x out]
      (ppt/pprint x (io/writer out)))
    (fn [x out]
      (binding [*out* (io/writer out)]
        (pr x)
        (println)))))

(defn ->msgpack-reader
  [_]
  mp/unpack)

(defn ->msgpack-writer
  [_]
  (fn [data out]
    (mp/pack-stream data out)
    (binding [*out* (io/writer out)]
      (flush))))

(defn ->line-reader
  [_]
  (fn [in]
    (line-seq (io/reader in))))

(defn ->line-writer
  [_]
  (fn [data out]
    (binding [*out* (io/writer out)]
      (if (seqable? data)
        (doseq [line (seq data)]
          (println line))
        (println data)))))

(defn ->text-reader
  [_]
  (fn [in]
    (slurp in)))

(defn ->text-writer
  [_]
  (fn [data out]
    (binding [*out* (io/writer out)]
      (println data))))

(defn- csv->maps
  [[headers & data]]
  (let [headers (map keyword headers)
        row->map (fn [m k v]
                   (assoc m (nth headers k) v))]
    (for [row data]
      (reduce-kv row->map {} row))))

(defn ->csv-reader
  [{:keys [csv-no-header]}]
  (fn [in]
    (with-open [r (io/reader in)]
      (let [data (csv/read-csv r)]
        (doall data)
        (if csv-no-header
          data
          (csv->maps data))))))

(defn- maps->csv
  [[row1 :as data]]
  (let [headers (keys row1)]
    (concat [(map name headers)]
            (for [row data]
              (for [h headers]
                (get row h))))))

(defn ->csv-writer
  [_]
  (fn [[row1 :as data] out]
    (let [data (if (map? row1)
                 (maps->csv data)
                 data)]
      (with-open [w (io/writer out)]
        (csv/write-csv w data)))))

(def formats
  {"json"    {:->reader ->json-reader
              :->writer ->json-writer}
   "edn"     {:->reader ->edn-reader
              :->writer ->edn-writer}
   "msgpack" {:->reader ->msgpack-reader
              :->writer ->msgpack-writer}
   "lines"   {:->reader ->line-reader
              :->writer ->line-writer}
   "text"    {:->reader ->text-reader
              :->writer ->text-writer}
   "csv"     {:->reader ->csv-reader
              :->writer ->csv-writer}})

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

(ns cq.formats
  (:require [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.data.xml :as xml]
            [hickory.core :as html]
            [hickory.render :refer [hickory-to-html]]
            [clojure.edn :as edn]
            [clojure.pprint :as ppt]
            [clojure.java.io :as io]
            [msgpack.core :as mp]
            [msgpack.clojure-extensions]
            [clj-yaml.core :as yaml]
            [cognitect.transit :as transit]
            [puget.printer :as puget])
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
    (edn/read {:default tagged-literal} (PushbackReader. (io/reader in)))))

(defn ->edn-writer
  [{:keys [pretty color]}]
  (if pretty
    (fn [x out]
      (binding [*out* (io/writer out)]
        (if color
          (puget/cprint x)
          (puget/pprint x))))
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
  slurp)

(defn ->text-writer
  [_]
  (fn [data out]
    (binding [*out* (io/writer out)]
      (println data))))

(defn- csv->maps
  [[headers & data]
   {:keys [key-fn] :or {key-fn keyword}}]
  (let [headers (map key-fn headers)
        row->map (fn [m k v]
                   (assoc m (nth headers k) v))]
    (for [row data]
      (reduce-kv row->map {} row))))

(defn ->csv-reader
  [{:keys [csv-no-header] :as opts}]
  (fn [in]
    (with-open [r (io/reader in)]
      (let [data (csv/read-csv r)]
        (doall data)
        (if csv-no-header
          data
          (csv->maps data opts))))))

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

;; TODO: switch to yaml/parse-stream once https://github.com/clj-commons/clj-yaml/pull/20 is merged
(defn ->yaml-reader
  [{:keys [yaml-unsafe yaml-keywords yaml-max-aliases-for-collections yaml-allow-recursive-keys yaml-allow-duplicate-keys]
    :or {yaml-keywords true}}]
  (let [opts [:unsafe yaml-unsafe
              :keywords yaml-keywords
              :max-aliases-for-collections yaml-max-aliases-for-collections
              :allow-recursive-keys yaml-allow-recursive-keys
              :allow-duplicate-keys yaml-allow-duplicate-keys]]
    (fn [in]
      (apply yaml/parse-string (slurp (io/reader in)) opts))))

(defn ->yaml-writer
  [{:keys [yaml-flow-style]
    :or {yaml-flow-style :auto}}]
  (let [opts [:dumper-options {:flow-style (keyword yaml-flow-style)}]]
    (fn [data out]
      (binding [*out* (io/writer out)]
        (print (apply yaml/generate-string data opts))
        (flush)))))

(defn ->transit-reader
  [{:keys [transit-format-in]
    :or {transit-format-in :json}}]
  (fn [in]
    (-> in
        (transit/reader (keyword transit-format-in))
        transit/read)))

(defn ->transit-writer
  [{:keys [transit-format-out]
    :or {transit-format-out :json}}]
  (fn [data out]
    (-> out
        (transit/writer (keyword transit-format-out))
        (transit/write data))))

(defn ->xml-reader
  [_]
  (fn [in]
    (xml/parse (io/reader in))))

(defn ->xml-writer
  [{:keys [pretty]}]
  (let [emit (if pretty xml/indent xml/emit)]
    (fn [x out]
      (with-open [w (io/writer out)]
        (emit x w)))))

(defn ->html-reader
  [_]
  (fn [in]
    (html/as-hickory (html/parse (slurp (io/reader in))))))

(defn ->html-writer
  [_]
  (fn [x out]
    (binding [*out* (io/writer out)]
      (print (hickory-to-html x))
      (flush))))

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
              :->writer ->csv-writer}
   "yaml"    {:->reader ->yaml-reader
              :->writer ->yaml-writer}
   "transit" {:->reader ->transit-reader
              :->writer ->transit-writer}
   "xml"     {:->reader ->xml-reader
              :->writer ->xml-writer}
   "html"    {:->reader ->html-reader
              :->writer ->html-writer}})

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

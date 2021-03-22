(ns cq.formats-test
  (:require [cq.formats :as sut]
            [clojure.test :refer :all]
            [clojure.java.io :as io])
  (:import [java.io ByteArrayInputStream BufferedInputStream PrintStream ByteArrayOutputStream]))

(defn- to-out-stream
  [w data]
  (let [out-stream (ByteArrayOutputStream.)]
    (w data (PrintStream. out-stream))
    out-stream))

(defn- test-reader-bytes
  [->reader opts data]
  ((->reader opts) (BufferedInputStream. (ByteArrayInputStream. data))))

(defn- test-reader-str
  [->reader opts data]
  (test-reader-bytes ->reader opts (.getBytes data)))

(defn- test-writer-str
  [->writer opts data]
  (.toString (to-out-stream (->writer opts) data)))

(defn- test-writer-bytes
  [->writer opts data]
  (.toByteArray (to-out-stream (->writer opts) data)))

(deftest text
  (testing "reader"
    (is (= "hello" (test-reader-str sut/->text-reader nil "hello"))))

  (testing "writer"
    (is (= "hello\n" (test-writer-str sut/->text-writer nil "hello")))
    (is (= "hello\n\n" (test-writer-str sut/->text-writer nil "hello\n")))))

(deftest lines
  (testing "reader"
    (is (= ["a" "bc"] (test-reader-str sut/->line-reader nil "a\nbc")))
    (is (= ["a" "bc"] (test-reader-str sut/->line-reader nil "a\nbc\n")))
    (is (= ["a" "bc" ""] (test-reader-str sut/->line-reader nil "a\nbc\n\n"))))

  (testing "writer"
    (is (= "a\nbc\n" (test-writer-str sut/->line-writer nil ["a" "bc"])))
    (is (= "a\nbc\n\n" (test-writer-str sut/->line-writer nil ["a" "bc" ""])))))

(deftest json
  (testing "reader"
    (is (= {:a {:b [1 2 3]}} (test-reader-str sut/->json-reader nil "{\"a\": {\"b\": [1, 2, 3]}}"))))

  (testing "writer"
    (is (= "{\"a\":{\"b\":[1,2,3]}}\n" (test-writer-str sut/->json-writer nil {:a {:b [1 2 3]}})))

    (testing "pretty"
      (is (= "{\"a\":{\"b\":[1, 2, 3]}}\n" (test-writer-str sut/->json-writer {:pretty true} {:a {:b [1 2 3]}}))))))

(deftest edn
  (testing "reader"
    (is (= {:a {:b [1 2 3]}} (test-reader-str sut/->edn-reader nil "{:a {:b [1 2 3]}}"))))

  (testing "writer"
    (is (= "{:a {:b [1 2 3]}}\n" (test-writer-str sut/->edn-writer nil {:a {:b [1 2 3]}})))))

(defn resource->bytes [file]
  (with-open [xin (io/input-stream (io/resource file))
              xout (java.io.ByteArrayOutputStream.)]
    (io/copy xin xout)
    (.toByteArray xout)))

;; TODO: msgpack needs proper keywordise support
(deftest msgpack
  (testing "reader"
    (is (= {"a" {"b" [1 2 3]}} (test-reader-bytes sut/->msgpack-reader nil (resource->bytes "test.mp.golden")))))

  (testing "writer"
    (is (= (seq (resource->bytes "test.mp.golden")) (seq (test-writer-bytes sut/->msgpack-writer nil {"a" {"b" [1 2 3]}}))))))

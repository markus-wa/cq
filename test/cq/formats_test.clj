(ns cq.formats-test
  (:require [cq.formats :as sut]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.core :as html])
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
  (str/replace (.toString (to-out-stream (->writer opts) data)) #"\r\n" "\n"))

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
    (is (= "{:a {:b [1 2 3]}}\n" (test-writer-str sut/->edn-writer nil {:a {:b [1 2 3]}})))

    (testing "pretty"
      (is (= "{:a {:b [1 2 3],\n     :c [2 3 {:something :long}],\n     :d [1 2 {:something :even/longer}]}}\n"
             (test-writer-str sut/->edn-writer {:pretty true :color false} {:a {:b [1 2 3] :c [2 3 {:something :long}] :d [1 2 {:something :even/longer}]}})))

      (testing "color"
        (is (= "\u001B[1;31m{\u001B[0m\u001B[1;33m:a\u001B[0m \u001B[1;31m{\u001B[0m\u001B[1;33m:b\u001B[0m \u001B[1;31m[\u001B[0m\u001B[36m1\u001B[0m \u001B[36m2\u001B[0m \u001B[36m3\u001B[0m\u001B[1;31m]\u001B[0m,\n     \u001B[1;33m:c\u001B[0m \u001B[1;31m[\u001B[0m\u001B[36m2\u001B[0m \u001B[36m3\u001B[0m \u001B[1;31m{\u001B[0m\u001B[1;33m:something\u001B[0m \u001B[1;33m:long\u001B[0m\u001B[1;31m}\u001B[0m\u001B[1;31m]\u001B[0m,\n     \u001B[1;33m:d\u001B[0m \u001B[1;31m[\u001B[0m\u001B[36m1\u001B[0m \u001B[36m2\u001B[0m \u001B[1;31m{\u001B[0m\u001B[1;33m:something\u001B[0m \u001B[1;33m:even/longer\u001B[0m\u001B[1;31m}\u001B[0m\u001B[1;31m]\u001B[0m\u001B[1;31m}\u001B[0m\u001B[1;31m}\u001B[0m\n"
               (test-writer-str sut/->edn-writer {:pretty true :color true} {:a {:b [1 2 3] :c [2 3 {:something :long}] :d [1 2 {:something :even/longer}]}})))))))

(comment
  ;; simple utility for printing an ANSI escaped sequence for a test constant
  ;; ANSI uses the ESC character for escape codes and it is lost in printing
  (let [x {:a {:b [1 2 3] :c [2 3 {:something :long}] :d [1 2 {:something :even/longer}]}}
        s (test-writer-str sut/->edn-writer {:pretty true :color true} x)]
    (str/join (for [c s]
                (if (= 27 (int c))
                  "ESC" ; replace with \u001B and you get a test constant
                  c)))))

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

(deftest csv
  (testing "reader"
    (is (= [{:a "1" :b "2"} {:a "3" :b "4"}]
           (test-reader-str sut/->csv-reader nil "a,b\n1,2\n3,4")))
    (is (= [["1" "2"] ["3" "4"]]
           (test-reader-str sut/->csv-reader {:csv-no-header true} "1,2\n3,4"))))
  (testing "writer"
    (is (= "a,b\n1,2\n3,4\n" (test-writer-str sut/->csv-writer nil [{:a 1, :b 2} {:a 3, :b 4}])))
    (is (= "1,2\n3,4\n" (test-writer-str sut/->csv-writer nil [[1 2] [3 4]])))))

(deftest yaml
  (testing "reader"
    (is (= {:a {:b [1 2 3]}} (test-reader-str sut/->yaml-reader nil "{\"a\": {\"b\": [1, 2, 3]}}")))
    (is (= {:a {:b [1 2 3]}} (test-reader-str sut/->yaml-reader nil "a:\n  b: [1, 2, 3]"))))

  (testing "writer"
    (is (= "a:\n  b: [1, 2, 3]\n" (test-writer-str sut/->yaml-writer nil {:a {:b [1 2 3]}})))

    (testing "pretty"
      (is (= "a:\n  b:\n  - 1\n  - 2\n  - 3\n" (test-writer-str sut/->yaml-writer {:yaml-flow-style :block} {:a {:b [1 2 3]}}))))))

(deftest transit
  (testing "reader"
    (is (= {:a {:b [1 2 3]}} (test-reader-str sut/->transit-reader nil "[\"^ \",\"~:a\",[\"^ \",\"~:b\",[1,2,3]]]"))))

  (testing "writer"
    (is (= "[\"^ \",\"~:a\",[\"^ \",\"~:b\",[1,2,3]]]" (test-writer-str sut/->transit-writer nil {:a {:b [1 2 3]}})))))

(def test-xml-str
  "<?xml version='1.0' encoding='utf-8'?>
     <!DOCTYPE html SYSTEM 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>
     <html xmlns='http://www.w3.org/1999/xhtml'>
       <article>Hello</article>
     </html>")

(def test-xml-data
  {:tag :xmlns.http%3A%2F%2Fwww.w3.org%2F1999%2Fxhtml/html,
   :attrs {},
   :content
   '({:tag :xmlns.http%3A%2F%2Fwww.w3.org%2F1999%2Fxhtml/article,
      :attrs {},
      :content ("Hello")})})

(deftest xml
  (testing "reader"
    (is (= test-xml-data
           (test-reader-str sut/->xml-reader nil test-xml-str))))

  (testing "writer"
    (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a:html xmlns:a=\"http://www.w3.org/1999/xhtml\"><a:article>Hello</a:article></a:html>"
           (test-writer-str sut/->xml-writer nil test-xml-data)))

    (testing "pretty"
      (is (= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<a:html xmlns:a=\"http://www.w3.org/1999/xhtml\">
  <a:article>Hello</a:article>
</a:html>\n"
             (test-writer-str sut/->xml-writer {:pretty true} test-xml-data))))))

(def test-html-str
  "<p>hello</p>")

(def test-html-data
  {:type :document
   :content
   [{:type  :element
     :attrs nil
     :tag   :html
     :content
     [{:type  :element
       :attrs nil
       :tag   :head :content nil}
      {:type  :element
       :attrs nil
       :tag   :body :content
       [{:type    :element
         :attrs   nil
         :tag     :p
         :content ["hello"]}]}]}]})

(deftest html
  (testing "reader"
    (is (= test-html-data
           (test-reader-str sut/->html-reader nil test-html-str))))

  (testing "writer"
    (is (= "<html><head></head><body><p>hello</p></body></html>"
           (test-writer-str sut/->html-writer nil test-html-data)))))

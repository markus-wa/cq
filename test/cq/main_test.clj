(ns cq.main-test
  (:require [clojure.test :refer :all]
            [cq.main :as sut])
  (:import [java.io ByteArrayInputStream BufferedInputStream PrintStream ByteArrayOutputStream]))

(defn- stdin-stub [data]
  (BufferedInputStream. (ByteArrayInputStream. (.getBytes data))))

(defn run-main
  [input & args]
  (let [stdout-stub (ByteArrayOutputStream.)]
    (binding [sut/*stdin* (stdin-stub input)
              sut/*stdout* (PrintStream. stdout-stub)]
      (apply sut/-main args)
      (read-string (.toString stdout-stub)))))

(deftest main
  (testing "simple query works"
    (is (= '(2 3 4)
           (run-main "a: {b: [1, 2, 3]}" ":a :b" "(map inc)"))))

  (testing "readers"
    (testing "#|"
      (is (= 2
             (run-main "a: {b: 2}" ":a" "#|" "(:b .)"))))

    (testing "#map"
      (is (= '({:v 1} {:v 2} {:v 3})
             (run-main "a: {b: [1, 2, 3]}" ":a :b" "#map {:v .}"))))

    (testing "#&"
      (is (= {:first 1, :second 2, :third 3}
             (run-main "a: {b: [1, 2, 3]}" ":a :b" "#& ([first second third] {:first first :second second :third third})"))))

    (testing "#f"
      (is (= {2 :b, 3 :c}
             (run-main "a: {b: 2, c: 3}" ":a (map-kv #f [%2 %1])"))))))

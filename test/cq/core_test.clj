(ns cq.core-test
  (:require [cq.core :as sut]
            [clojure.test :refer :all]))

(deftest thread-redirection
  (testing "->"
    (is (= ["http:" "" "example.com" "some" "path"]
           (sut/query {:a "http://example.com/some/path"} '[:a -> (str/split #"/")]))))

  (testing "->>"
    (is (= ["HTTP:" "" "EXAMPLE.COM" "SOME" "PATH"]
           (sut/query "http://example.com/some/path" '[-> (str/split #"/") ->> (map str/upper-case)]))))

  (testing "some->"
    (is (nil? (sut/query {} '[some-> :b (str/split #"/")]))))

  (testing "some->>"
    (is (nil? (sut/query {} '[some->> :b (map inc)]))))

  (testing "as->"
    (is (= 1 (sut/query {:a 1} '[as-> v (:a v)])))))

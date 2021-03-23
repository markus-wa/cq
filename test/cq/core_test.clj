(ns cq.core-test
  (:require [cq.core :as sut]
            [clojure.test :refer :all]))

(deftest thread-redirection
  (testing "->"
    (is (= ["http:" "" "example.com" "some" "path"]
           (sut/query {:a "http://example.com/some/path"} '[:a -> (str/split #"/")]))))
  (testing "->>"
    (is (= ["HTTP:" "" "EXAMPLE.COM" "SOME" "PATH"]
           (sut/query "http://example.com/some/path" '[-> (str/split #"/") ->> (map str/upper-case)])))))

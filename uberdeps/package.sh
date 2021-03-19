#!/bin/bash -e
cd "$( dirname "${BASH_SOURCE[0]}" )/.."
mkdir -p classes
clj -M -e "(compile 'cq.main)"

cd "$( dirname "${BASH_SOURCE[0]}" )"
clojure -M -m uberdeps.uberjar --deps-file ../deps.edn --target ../target/cq.jar --main-class cq.main

#!/bin/bash -e
cd "$( dirname "${BASH_SOURCE[0]}" )/.."
mkdir -p classes
clojure -M -e "(compile 'cq.main)"

cd "$( dirname "${BASH_SOURCE[0]}" )"
clojure -M -m uberdeps.uberjar --deps-file ../deps.edn --target ../target/cq.jar --main-class cq.main

clojure -M -m clj-bin.main --custom-preamble-script preamble.sh --jar ../target/cq.jar --out ../target/cq-jvm
clojure -M -m clj-bin.main --custom-preamble-script preamble.bat --jar ../target/cq.jar --out ../target/cq-jvm.exe

native-image --report-unsupported-elements-at-runtime \
             --initialize-at-build-time \
             --no-server \
             -jar ../target/cq.jar \
             -H:Name=../target/cq

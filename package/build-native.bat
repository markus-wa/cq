cd %~dp0

native-image --report-unsupported-elements-at-runtime ^
             --initialize-at-build-time ^
             --no-server ^
             -jar ../target/cq.jar ^
             -H:Name=../target/cq

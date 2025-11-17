#!/usr/bin/env bash
./gradlew quarkusDev --quarkus-args="-l app_config.yaml" -PunsafeCompile=true

#The following also works, but is slow and provides lesser dev-mode features
#../../../gradlew :lattice:samples:rest-json-lattice-sample:run --args="-l app_config.yaml" -PunsafeCompile=true

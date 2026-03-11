#!/usr/bin/env bash
../../../../../../gradlew :lattice:samples:rest:json:dropwizard:rest-json-dropwizard-lattice-sample:run --args="-l app_config.yaml" -PunsafeCompile=true

#The following also works, but is slow and provides lesser dev-mode features
#../../../gradlew :lattice:samples:rest:json:quarkus:rest-json-quarkus-lattice-sample:run --args="-l app_config.yaml" -PunsafeCompile=true

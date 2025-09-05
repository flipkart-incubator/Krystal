#!/usr/bin/env bash
../../../gradlew :lattice:samples:rest-json-lattice-sample:run --args="-l app_config.yaml" -PunsafeCompile=true
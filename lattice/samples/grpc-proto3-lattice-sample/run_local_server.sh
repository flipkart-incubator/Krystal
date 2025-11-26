#!/usr/bin/env bash
../../../gradlew :lattice:samples:grpc-proto3-lattice-sample:run --args="-l app_config.yaml" -PunsafeCompile=true
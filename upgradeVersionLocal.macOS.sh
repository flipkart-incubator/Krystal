#!/usr/bin/env bash
#Unsafe compile temporarily needed due to a checker-framework issue
./gradlew :vajram:vajram-codegen:publishToMavenLocal -PunsafeCompile=true --no-build-cache &&\
./gradlew publishToMavenLocal -PunsafeCompile=true --no-build-cache

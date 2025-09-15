#!/usr/bin/env bash
./gradlew publishToMavenLocal -PunsafeCompile=true
./gradlew compileJava -PunsafeCompile=true
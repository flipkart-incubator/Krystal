#!/usr/bin/env bash
sed -i '' 's/com.flipkart.krystal.currentVersion/com.flipkart.krystal.previousVersion/g' build.gradle \
&&
#Unsafe compile temporarily needed due to a checker-framework issue
./gradlew publishToMavenLocal -PunsafeCompile=true --no-build-cache \
&&
sed -i '' 's/com.flipkart.krystal.previousVersion/com.flipkart.krystal.currentVersion/g' build.gradle \
&&
#Unsafe compile temporarily needed due to a checker-framework issue
./gradlew publishToMavenLocal -PunsafeCompile=true --no-build-cache

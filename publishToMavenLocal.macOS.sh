#!/usr/bin/env bash
sed -i '' 's/com.flipkart.krystal.currentVersion/com.flipkart.krystal.previousVersion/g' build.gradle
./gradlew publishToMavenLocal -PunsafeCompile=true --no-build-cache #Unsafe compile temporarily needed due to a checker-framework issue
sed -i '' 's/com.flipkart.krystal.previousVersion/com.flipkart.krystal.currentVersion/g' build.gradle
./gradlew publishToMavenLocal -PunsafeCompile=true --no-build-cache #Unsafe compile temporarily needed due to a checker-framework issue

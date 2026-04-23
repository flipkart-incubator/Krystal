#!/usr/bin/env bash
sed -i '' 's/com.flipkart.krystal.currentVersion/com.flipkart.krystal.previousVersion/g' build.gradle \
&&
#Unsafe compile temporarily needed due to a checker-framework issue
./gradlew :vajram:vajram-codegen:publishToMavenLocal -PunsafeCompile=true \
&&
sed -i '' 's/com.flipkart.krystal.previousVersion/com.flipkart.krystal.currentVersion/g' build.gradle \
&&
#Unsafe compile temporarily needed due to a checker-framework issue
./gradlew compileJava publishToMavenLocal -PunsafeCompile=true

buildscript {
    repositories {
        mavenLocal()
    }
}

plugins {
    id("com.flipkart.mojopublish") version "0.0.1-SNAPSHOT"
    id("java-library")
    id("idea")
    id("maven-publish")
}

group = "com.flipkart.krystalrelease"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.flipkart.krystalrelease"
            artifactId = "mod1"
            from(components["java"])
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")
}

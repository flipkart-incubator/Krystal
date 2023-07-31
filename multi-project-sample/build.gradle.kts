plugins {
    id("maven-publish")
    id("com.flipkart.mojopublish") version "0.0.1-SNAPSHOT"
    id("java-library")
    id("idea")
}

group = "com.flipkart.krystalrelease"

repositories {
    mavenLocal()
    mavenCentral()
    maven {url=uri("https://clojars.org/repo")}
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")
    apply(plugin = "com.flipkart.mojopublish")

    repositories {
      mavenLocal()
      mavenCentral()
    }
}

plugins {
    id("maven-publish")
    id("java-library")
    id("idea")
    id("com.flipkart.mojopublish") version "1.0.0"
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

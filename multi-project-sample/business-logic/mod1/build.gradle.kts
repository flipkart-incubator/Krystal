buildscript {
    repositories {
        mavenLocal()
    }
}

plugins {
    id("com.flipkart.krystalrelease") version "0.0.1-SNAPSHOT"
}

group = "org.example"

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

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
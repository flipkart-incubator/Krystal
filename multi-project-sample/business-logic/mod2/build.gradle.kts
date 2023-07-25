buildscript {
    repositories {
        mavenLocal()
    }
}
plugins {
    id("com.flipkart.krystalrelease") version "0.0.1-SNAPSHOT"
}

group = "com.flipkart.krystalrelease"
version = "0.0.1-multiProjRelease+11111"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":business-logic:mod1"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.flipkart.krystalrelease"
            artifactId = "mod2"
            version = version
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
buildscript {
    repositories {
        mavenLocal()
    }
}
plugins {
    id("com.flipkart.krystalrelease") version "0.0.1-SNAPSHOT"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":business-logic:mod1"))
    implementation(project(":business-logic:mod2"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
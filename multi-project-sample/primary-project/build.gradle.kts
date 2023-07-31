plugins {
    id("java-library")
}

group = "com.flipkart"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.flipkart.krystalrelease"
            artifactId = "primary-project"
            from(components["java"])
        }
    }
}
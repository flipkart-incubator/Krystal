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

dependencies {
    implementation(project(":business-logic:mod1"))
    implementation(project(":business-logic:mod2"))
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
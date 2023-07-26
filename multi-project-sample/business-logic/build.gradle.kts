group = "com.flipkart.krystalrelease"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.flipkart.krystalrelease"
            artifactId = "mod1"
            from(components["java"])
        }
    }
}
group = "com.flipkart.krystalrelease"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.flipkart.krystalrelease"
            artifactId = "business-logic"
            from(components["java"])
        }
    }
}
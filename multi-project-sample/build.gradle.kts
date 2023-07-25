group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


subprojects {
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply(plugin = "maven-publish")
}

plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("java")
}

group = "com.flipkart.krystal"
version = project.findProperty("com.flipkart.krystal.currentVersion") ?: "0.0.0-SNAPSHOT"

description = "IntelliJ IDEA plugin for authoring Krystal Vajrams."

val localIntelliJPath =
    providers.gradleProperty("krystal.intellij.localPath")
        .orElse("/Applications/IntelliJ IDEA.app/Contents")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        local(localIntelliJPath)
        bundledPlugins("com.intellij.java")
    }
    compileOnly(project(":vajram:vajram-codegen-common"))
    compileOnly("jakarta.inject:jakarta.inject-api:2.0.1")
    compileOnly("org.checkerframework:checker-qual:3.48.4")
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    pluginConfiguration {
        name.set("Krystal")
        ideaVersion {
            sinceBuild.set("253")
        }
    }
}

tasks {
    test {
        useJUnit()
    }
}

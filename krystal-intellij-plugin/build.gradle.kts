plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("java")
}

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
    compileOnly("jakarta.inject:jakarta.inject-api")
    compileOnly("org.checkerframework:checker-qual")
    testCompileOnly(project(":vajram:vajram-codegen-common"))
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
        jvmArgumentProviders.clear()
        jvmArgs = emptyList()
        systemProperties.clear()
    }
}

extraJavaModuleInfo {
    failOnMissingModuleInfo = false
}

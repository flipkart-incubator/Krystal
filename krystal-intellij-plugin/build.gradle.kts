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
    // compileTestJava depends on instrumentCode, which downloads java-compiler-ant-tasks
    // from JetBrains Maven. Disable for unit-test compilation; re-enable for buildPlugin if needed.
    named("instrumentCode") { enabled = false }
    named("instrumentTestCode") { enabled = false }

    // IntelliJ Platform plugin configures the default `test` task with IDE sandbox JVM args.
    // Use plain JUnit for lightweight unit tests in this module.
    named("prepareTest") { enabled = false }
    named("prepareTestSandbox") { enabled = false }

    test {
        useJUnit()
        jvmArgumentProviders.clear()
        jvmArgs = emptyList()
        systemProperties.clear()
    }
}

extraJavaModuleInfo {
    failOnMissingModuleInfo = false
}

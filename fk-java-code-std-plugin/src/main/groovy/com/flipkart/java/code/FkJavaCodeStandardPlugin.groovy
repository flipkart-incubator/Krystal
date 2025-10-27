package com.flipkart.java.code

import com.diffplug.gradle.spotless.SpotlessExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

class FkJavaCodeStandardPlugin implements Plugin<Project> {

    public static final String UNSAFE_COMPILE_OPTION = "unsafeCompile"

    @Override
    void apply(Project project) {
        java(project)
        idea(project)
        tests(project)

        checkerFramework(project)
        spotless(project)
        errorProne(project)
    }

    private static tests(Project project) {
        junitPlatform(project)
        jacoco(project)
    }

    private static java(Project project) {
        project.pluginManager.apply('java')
    }

    private static idea(Project project) {
        project.pluginManager.apply('idea')
    }

    private static checkerFramework(Project project) {
        project.pluginManager.apply('org.checkerframework')
        CheckerFrameworkExtension checkerFramework = project.extensions.findByType(CheckerFrameworkExtension)

        checkerFramework.checkers = ["org.checkerframework.checker.nullness.NullnessChecker",
                                     "org.checkerframework.checker.calledmethods.CalledMethodsChecker",
                                     "org.checkerframework.checker.optional.OptionalChecker",
//                                   "org.checkerframework.checker.resourceleak.ResourceLeakChecker",
        ]
        checkerFramework.extraJavacArgs.add("-Astubs=${project.rootDir}/config/checker/stubs")
        checkerFramework.extraJavacArgs.add("-AskipFiles=/build/generated/")
        def checker_version = '3.48.4'
        project.dependencies.add('checkerFramework', "org.checkerframework:checker:${checker_version}")

        project.dependencies.add('compileOnly', "org.checkerframework:checker-qual:${checker_version}")
        project.dependencies.add('annotationProcessor', "org.checkerframework:checker-qual:${checker_version}")
        project.dependencies.constraints.add('implementation', "org.checkerframework:checker-qual:${checker_version}")
        project.dependencies.constraints.add('testImplementation', "org.checkerframework:checker-qual:${checker_version}")
        project.dependencies.constraints.add('testAnnotationProcessor', "org.checkerframework:checker-qual:${checker_version}")

        project.dependencies.add('annotationProcessor', "org.checkerframework:dataflow-errorprone:${checker_version}")

        project.dependencies.add('annotationProcessor', 'com.google.guava:guava')
        project.dependencies.add('annotationProcessor', 'javax.inject:javax.inject:1')
        project.dependencies.add('annotationProcessor', 'com.google.auto.value:auto-value-annotations:1.11.0')
        project.dependencies.add('annotationProcessor', 'com.github.ben-manes.caffeine:caffeine:3.1.8')
        project.dependencies.add('annotationProcessor', 'org.pcollections:pcollections:4.0.2')
        project.dependencies.add('annotationProcessor', 'com.github.kevinstern:software-and-algorithms:1.0')

        if (project.findProperty(UNSAFE_COMPILE_OPTION) == "true") {
            checkerFramework.skipCheckerFramework = true
        }
        project.configurations.configureEach {
            it.resolutionStrategy {
                force "org.checkerframework:checker-qual:${checker_version}"
            }
        }
    }

    private static spotless(Project project) {
        project.pluginManager.apply('com.diffplug.spotless')

        project.extensions.findByType(SpotlessExtension)
                .java {
                    target('src/*/java/**/*.java',
                            'build/generated/sources/annotationProcessor/**/*.java')
                    googleJavaFormat()
                }

        project.tasks.named('spotlessJava').configure { mustRunAfter('compileJava') }
        project.tasks.named('spotlessJava').configure { mustRunAfter('compileTestJava') }

        project.afterEvaluate(p -> {
            p.tasks.named('assemble') {
                it.dependsOn('spotlessApply')
            }
        })
        if (project.findProperty(UNSAFE_COMPILE_OPTION) == "true") {
            project.tasks.named('spotlessCheck').configure {
                enabled = false
            }
        }
    }

    private static errorProne(Project project) {
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_core:2.38.0')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_check_api:2.38.0')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_annotation:2.38.0')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_annotations:2.38.0')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_type_annotations:2.38.0')

        project.pluginManager.apply('net.ltgt.errorprone')

        project.dependencies.add('errorprone', 'com.google.errorprone:error_prone_core:2.38.0')
        project.tasks.compileTestJava.configure { JavaCompile task -> task.options.errorprone.enabled = false }
        project.tasks
                .withType(JavaCompile)
                .configureEach { JavaCompile task -> task.options.errorprone.disableWarningsInGeneratedCode = true }

        if (project.findProperty(UNSAFE_COMPILE_OPTION) == "true") {
            project.tasks
                    .withType(JavaCompile)
                    .configureEach { JavaCompile task -> task.options.errorprone.enabled = false }
        } else {
            project.tasks.compileJava.configure { JavaCompile task -> task.options.errorprone.disable("StringConcatToTextBlock") }
        }
    }

    private static void junitPlatform(Project project) {
        project.dependencies.add('testImplementation', project.getDependencies().platform('org.junit:junit-bom:6.0.0'))
        project.dependencies.add('testRuntimeOnly', 'org.junit.platform:junit-platform-launcher')
        project.dependencies.add('testImplementation', 'org.junit.jupiter:junit-jupiter')
        project.test {
            useJUnitPlatform()
        }
    }

    private static jacoco(Project project) {
        project.pluginManager.apply('jacoco')

        project.extensions.findByType(JacocoPluginExtension).setToolVersion("0.8.12")
    }
}

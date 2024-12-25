package com.flipkart.java.code

import com.diffplug.gradle.spotless.SpotlessExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

class FkJavaCodeStandard implements Plugin<Project> {
    @Override
    void apply(Project project) {
        java(project)
        idea(project)
        checkerFramework(project)
        spotless(project)
        errorProne(project)

        tests(project)
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
//                                     "org.checkerframework.checker.resourceleak.ResourceLeakChecker",
        ]
        checkerFramework.extraJavacArgs.add("-Astubs=${project.rootDir}/config/checker/stubs")
        checkerFramework.extraJavacArgs.add("-AskipFiles=/build/generated/")

        project.dependencies.add('checkerFramework', 'org.checkerframework:checker:3.48.3')
        project.dependencies.add('compileOnly', 'org.checkerframework:checker-qual:3.48.3')
        project.dependencies.add('annotationProcessor', 'org.checkerframework:dataflow-errorprone:3.48.3')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_core:2.27.1')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_check_api:2.27.1')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_annotation:2.27.1')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_annotations:2.27.1')
        project.dependencies.add('annotationProcessor', 'com.google.errorprone:error_prone_type_annotations:2.27.1')
        project.dependencies.add('annotationProcessor', 'com.google.guava:guava:33.4.0-jre')
        project.dependencies.add('annotationProcessor', 'javax.inject:javax.inject:1')
        project.dependencies.add('annotationProcessor', 'com.google.auto.value:auto-value-annotations:1.11.0')
        project.dependencies.add('annotationProcessor', 'com.github.ben-manes.caffeine:caffeine:3.1.8')
        project.dependencies.add('annotationProcessor', 'org.pcollections:pcollections:4.0.2')
        project.dependencies.add('annotationProcessor', 'com.github.kevinstern:software-and-algorithms:1.0')


    }

    private static spotless(Project project) {
        project.pluginManager.apply('com.diffplug.spotless')

        project.extensions.findByType(SpotlessExtension)
                .java {
                    target('src/*/java/**/*.java',
                            'build/generated/sources/annotationProcessor/**/*.java')
                    googleJavaFormat()
                }

        project.afterEvaluate(p -> {
            p.tasks.named('spotlessCheck') {
                it.dependsOn('spotlessApply')
            }
        })

        project.tasks.named('spotlessJava').configure { mustRunAfter('compileJava') }
        project.tasks.named('spotlessJava').configure { mustRunAfter('compileTestJava') }
    }

    private static errorProne(Project project) {
        project.pluginManager.apply('net.ltgt.errorprone')

        project.dependencies.add('errorprone', 'com.google.errorprone:error_prone_core:2.27.1')
    }

    private static void junitPlatform(Project project) {
        project.test {
            useJUnitPlatform()
        }
    }

    private static jacoco(Project project) {
        project.pluginManager.apply('jacoco')

        project.extensions.findByType(JacocoPluginExtension).setToolVersion("0.8.12")
    }
}

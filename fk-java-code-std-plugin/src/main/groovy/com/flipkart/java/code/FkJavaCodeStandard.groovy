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
                                     "org.checkerframework.checker.resourceleak.ResourceLeakChecker",
                                     "org.checkerframework.checker.fenum.FenumChecker"]
        checkerFramework.extraJavacArgs.add("-Astubs=${project.rootDir}/config/checker/stubs")
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

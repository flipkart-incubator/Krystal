package com.flipkart.java.code

import com.diffplug.gradle.spotless.SpotlessExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

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

    static java(Project project) {
        project.pluginManager.apply('java')
    }

    static idea(Project project) {
        project.pluginManager.apply('idea')
    }

    static checkerFramework(Project project) {
        project.pluginManager.apply('org.checkerframework')
        CheckerFrameworkExtension extension = project.extensions.findByType(CheckerFrameworkExtension)

        extension.checkers = ["org.checkerframework.checker.nullness.NullnessChecker",
                              "org.checkerframework.checker.calledmethods.CalledMethodsChecker"]
    }

    static spotless(Project project) {
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

    static errorProne(Project project) {
        project.pluginManager.apply('net.ltgt.errorprone')

        project.dependencies.add('errorprone', 'com.google.errorprone:error_prone_core:2.27.1')
    }

    static tests(Project project) {
        project.test {
            useJUnitPlatform()
        }
    }
}

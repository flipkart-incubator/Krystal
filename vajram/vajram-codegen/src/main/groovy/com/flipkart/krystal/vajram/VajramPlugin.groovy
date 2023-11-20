package com.flipkart.krystal.vajram

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VajramPlugin implements Plugin<Project> {


    void apply(Project project) {

        def vajramBuildDir = '/generated/sources/annotationProcessor/java'
        String mainGeneratedSrcDir = project.buildDir.getPath() + vajramBuildDir + '/main'
        String testGeneratedSrcDir = project.buildDir.getPath() + vajramBuildDir + '/test'

        project.sourceSets {
            main {
                java {
                    srcDirs = ['src/main/java', mainGeneratedSrcDir]
                }
            }
            test {
                java {
                    srcDirs = ['src/test/java', testGeneratedSrcDir]
                }
            }
        }

        project.tasks.register('codeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            source project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(mainGeneratedSrcDir))
            outputs.upToDateWhen { false }
        }

        project.tasks.register('codeGenVajramImpls', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            dependsOn 'codeGenVajramModels'
            source project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(mainGeneratedSrcDir))
            options.compilerArgs += ['-proc:only', '-processor', 'com.flipkart.krystal.vajram.codegen.VajramImplGenProcessor']
            outputs.upToDateWhen { false }
        }

        project.tasks.compileJava.dependsOn 'codeGenVajramImpls'

        project.tasks.register('testCodeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            mustRunAfter it.project.tasks.compileJava
            source project.sourceSets.test.allSource.srcDirs + project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.testCompileClasspath + project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileTestJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(testGeneratedSrcDir))
            outputs.upToDateWhen { false }
        }

        project.tasks.register('testCodeGenVajramImpls', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            dependsOn 'testCodeGenVajramModels'
            source project.sourceSets.test.allSource.srcDirs + project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.testCompileClasspath + project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileTestJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(testGeneratedSrcDir))
            options.compilerArgs += ['-proc:only', '-processor', 'com.flipkart.krystal.vajram.codegen.VajramImplGenProcessor']
            outputs.upToDateWhen { false }
        }

        project.tasks.named("jar").configure { it.dependsOn("compileJava") }
    }
}
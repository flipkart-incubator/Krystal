package com.flipkart.krystal.vajram

import com.flipkart.krystal.vajram.codegen.VajramCodeGenFacade
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VajramPlugin implements Plugin<Project> {
    void apply(Project project) {

        String mainGeneratedSrcDir = project.buildDir.getPath() + '/generated/sources/vajrams/main/java/'
        String testGeneratedSrcDir = project.buildDir.getPath() + '/generated/sources/vajrams/test/java/'

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

        String compiledMainDir = project.buildDir.getPath() + '/classes/java/main/'
        String compiledTestDir = project.buildDir.getPath() + '/classes/java/test/'

        project.tasks.register('codeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            source project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(mainGeneratedSrcDir))
            options.compilerArgs += ['-processor', 'com.flipkart.krystal.vajram.codegen.VajramModelGenProcessor']
        }


        // add a new task to generate vajram impl as this step needs to run after model generation
        // and compile
        project.tasks.register('codeGenVajramImpl') {
            group = 'krystal'
            dependsOn it.project.tasks.codeGenVajramModels
            print project.tasks.compileJava.destinationDirectory
            doLast {
                VajramCodeGenFacade.codeGenVajramImpl(
                        project.sourceSets.main.java.srcDirs,
                        compiledMainDir,
                        mainGeneratedSrcDir,
                        project.tasks.compileJava.classpath)
            }
        }

        project.tasks.compileJava.dependsOn 'codeGenVajramImpl'

        project.tasks.register('testCodeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            dependsOn it.project.tasks.compileJava
            source project.sourceSets.test.allSource.srcDirs + project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.testCompileClasspath + project.configurations.compileClasspath
            println 'Compile classpath: ' + classpath.toList()
            destinationDirectory = project.tasks.compileTestJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(testGeneratedSrcDir))
            mustRunAfter("createCheckerFrameworkManifest")
        }

        project.tasks.register('testCodeGenVajramImpl') {
            group = 'krystal'
            dependsOn it.project.tasks.testCodeGenVajramModels
            doLast {
                VajramCodeGenFacade.codeGenVajramImpl(
                        project.sourceSets.test.java.srcDirs,
                        compiledTestDir,
                        testGeneratedSrcDir,
                        project.tasks.compileTestJava.classpath)
            }
        }

        project.tasks.named("sourcesJar").configure { it.dependsOn("codeGenVajramImpl") }
        project.tasks.named("jar").configure { it.dependsOn("codeGenVajramImpl") }
    }
}
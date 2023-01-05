package com.flipkart.krystal.vajram

import com.flipkart.krystal.vajram.codegen.VajramModelsCodeGen
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
                    srcDir mainGeneratedSrcDir
                }
            }
            test {
                java {
                    srcDir testGeneratedSrcDir
                }
            }
        }

        project.tasks.register('codeGenVajramModels') {
            group = 'krystal'
            doLast {
                VajramModelsCodeGen.codeGenModels(
                        project.sourceSets.main.java.srcDirs,
                        mainGeneratedSrcDir)
            }
        }

        project.tasks.register('compileVajramModels', JavaCompile) {
            group = 'krystal'
            dependsOn it.project.tasks.codeGenVajramModels
            //Compile the generatedCode
            source project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
        }

        project.tasks.compileJava.dependsOn 'compileVajramModels'

        project.tasks.register('testCodeGenVajramModels') {
            group = 'krystal'
            doLast {
                VajramModelsCodeGen.codeGenModels(
                        project.sourceSets.test.java.srcDirs,
                        testGeneratedSrcDir)
            }
        }

        project.tasks.register('testCompileVajramModels', JavaCompile) {
            group = 'krystal'
            dependsOn it.project.tasks.testCodeGenVajramModels
            //Compile the generatedCode
            source project.sourceSets.test.allSource.srcDirs
            classpath = project.configurations.testCompileClasspath
            destinationDirectory = project.tasks.compileTestJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
        }

        project.tasks.compileTestJava.dependsOn 'testCodeGenVajramModels'

    }
}
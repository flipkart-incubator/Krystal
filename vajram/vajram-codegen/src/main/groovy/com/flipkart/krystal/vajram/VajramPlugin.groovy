package com.flipkart.krystal.vajram

import com.flipkart.krystal.vajram.codegen.VajramCodeGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VajramPlugin implements Plugin<Project> {
    void apply(Project project) {
        def generatedSrcDir = project.buildDir.getPath() + '/generated/sources/src/main/java/'

        project.sourceSets {
            generatedcode {
                java {
                    srcDirs = [generatedSrcDir]

                    //set compilation path so that IDEs can show the generated source without errors
                    compileClasspath = project.configurations.compileClasspath
                }
            }
        }

        project.tasks.register('codegenModels') {
            dependsOn project.tasks.classes
            doLast {
                VajramCodeGenTask.codeGenModels(project.tasks.compileJava.destinationDirectory.asFile.get().getPath(), generatedSrcDir)
            }
        }

        project.tasks.register('compileGeneratedModels', JavaCompile) {
            dependsOn it.project.tasks.codegenModels
            //Compile the generatedCode
            source = project.sourceSets.generatedcode.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
        }
    }
}
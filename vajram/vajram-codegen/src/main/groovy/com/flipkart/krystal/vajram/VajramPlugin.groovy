package com.flipkart.krystal.vajram

import com.flipkart.krystal.vajram.codegen.VajramCodeGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VajramPlugin implements Plugin<Project> {
    void apply(Project project) {
        String generatedSrcDir = project.buildDir.getPath() + '/generated/sources/src/main/java/'

        project.sourceSets {
            main {
                java {
                    srcDir generatedSrcDir
                }
            }
        }

        project.tasks.register('vajramModels') {
            group = 'codegen'
            dependsOn project.tasks.classes
            doLast {
                VajramCodeGenTask.codeGenModels(
                        project.tasks.compileJava.destinationDirectory.asFile.get().getPath(),
                        generatedSrcDir)
            }
        }

        project.tasks.register('vajramCompileModels', JavaCompile) {
            group = 'codegen'
            dependsOn it.project.tasks.vajramModels
            //Compile the generatedCode
            source project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.compileClasspath
            destinationDirectory = project.tasks.compileJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
        }

        project.tasks.jar.dependsOn project.tasks.vajramCompileModels
    }
}
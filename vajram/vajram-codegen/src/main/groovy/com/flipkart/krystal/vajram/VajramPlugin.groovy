package com.flipkart.krystal.vajram

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VajramPlugin implements Plugin<Project> {


    void apply(Project project) {

        def buildPhaseFile = "krystal_build_phase.txt"
        def vajramBuildDir = '/generated/sources/vajrams'
        String mainGeneratedSrcDir = project.buildDir.getPath() + vajramBuildDir + '/main/java/'
        String testGeneratedSrcDir = project.buildDir.getPath() + vajramBuildDir + '/test/java/'

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
//            doFirst {
//                project.logger.error("Creating krystal_build_phase.txt in ${mainGeneratedSrcDir} with text '${BuildPhase.CODEGEN_MODELS}'")
//                new File(mainGeneratedSrcDir, buildPhaseFile).text = BuildPhase.CODEGEN_MODELS.name()
//            }
//            doLast {
//                project.logger.error("Deleting krystal_build_phase.txt in ${mainGeneratedSrcDir} with text '${BuildPhase.CODEGEN_MODELS}'")
//                new File(mainGeneratedSrcDir, buildPhaseFile).delete()
//            }
        }

        // add a new task to generate vajram impl as this step needs to run after model generation
        // and compile
//        project.tasks.register('codeGenVajramImpls', JavaCompile) {
//            //Compile the generatedCode
//            group = 'krystal'
//            dependsOn it.project.tasks.codeGenVajramModels
//            source project.sourceSets.main.allSource.srcDirs
//            classpath = project.configurations.compileClasspath
//            destinationDirectory = project.tasks.compileJava.destinationDirectory
//            //For lombok processing of EqualsAndHashCode
//            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
//            options.generatedSourceOutputDirectory.fileValue(project.file(mainGeneratedSrcDir))
//            doFirst {
//                project.logger.error("Creating krystal_build_phase.txt in ${mainGeneratedSrcDir} with text '${BuildPhase.CODEGEN_IMPLS}'")
//                new File(mainGeneratedSrcDir, buildPhaseFile).text = BuildPhase.CODEGEN_IMPLS.name()
//            }
//            doLast {
//                project.logger.error("Deleting krystal_build_phase.txt in ${mainGeneratedSrcDir} with text '${BuildPhase.CODEGEN_IMPLS}'")
//                new File(mainGeneratedSrcDir, buildPhaseFile).delete()
//            }
//        }

        project.tasks.compileJava.dependsOn 'codeGenVajramModels'

        project.tasks.register('testCodeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            mustRunAfter it.project.tasks.compileJava
            source project.sourceSets.test.allSource.srcDirs + project.sourceSets.main.allSource.srcDirs
            classpath = project.configurations.testCompileClasspath + project.configurations.compileClasspath
            println 'Compile classpath: ' + classpath.toList()
            destinationDirectory = project.tasks.compileTestJava.destinationDirectory
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(testGeneratedSrcDir))
        }

//        project.tasks.register('testCodeGenVajramImpls') {
//            group = 'krystal'
//            dependsOn it.project.tasks.testCodeGenVajramModels
//            doLast {
//                VajramCodeGenFacade.codeGenVajramImpl(
//                        project.sourceSets.test.java.srcDirs,
//                        compiledTestDir,
//                        testGeneratedSrcDir,
//                        project.tasks.compileTestJava.classpath)
//            }
//        }

        project.tasks.named("sourcesJar").configure { it.dependsOn("codeGenVajramModels") }
        project.tasks.named("jar").configure { it.dependsOn("codeGenVajramModels") }
    }
}
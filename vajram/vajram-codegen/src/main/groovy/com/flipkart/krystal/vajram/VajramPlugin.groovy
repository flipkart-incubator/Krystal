package com.flipkart.krystal.vajram

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

import static com.flipkart.krystal.vajram.codegen.Constants.COGENGEN_PHASE_KEY
import static com.flipkart.krystal.vajram.codegen.models.CodegenPhase.IMPLS
import static com.flipkart.krystal.vajram.codegen.models.CodegenPhase.MODELS

class VajramPlugin implements Plugin<Project> {


    public static final String EMPTY_DIR = 'tmp/empty'

    void apply(Project project) {


        def srcGenDir = '/generated/sources/annotationProcessor/java'
        // Vajram models are generated in a different directory to keep the build phase inputs and outputs separate -
        // This enables better caching and to deterministically reuse previous build's outputs
        def vajramModelsGenDir = '/generated/sources/annotationProcessor/vajramModels/java'

        String mainSrcDir = 'src/main/java'
        String testSrcDir = 'src/test/java'

        String mainModelsGenDir = getBuildDir(project).getPath() + vajramModelsGenDir + '/main'
        String mainImplsGenDir = getBuildDir(project).getPath() + srcGenDir + '/main'

        String testModelsGenDir = getBuildDir(project).getPath() + vajramModelsGenDir + '/test'
        String testImplsGenDir = getBuildDir(project).getPath() + srcGenDir + '/test'

        project.sourceSets {
            main {
                java {
                    srcDirs = [mainSrcDir, mainModelsGenDir, mainImplsGenDir]
                }
            }
            test {
                java {
                    srcDirs = [testSrcDir, testModelsGenDir, testImplsGenDir]
                }
            }
        }

        project.tasks.register('codeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            source mainSrcDir
            classpath = project.configurations.compileClasspath
            // This is a 'proc:only' compile step. Which means, no .class files are generated. This means the destinationDirectory property
            // is not used by this step.
            // We reassign the `destinationDirectory` to a dummy empty directory so that the destination directory doesnot clash
            // with the full compile step. This is so that gradle caching works optimally - gradle doesn't cache outputs of tasks
            // which share output directories with other tasks -
            // See: https://docs.gradle.org/current/userguide/build_cache_concepts.html#concepts_overlapping_outputs
            getDestinationDirectory().set(project.getObjects().directoryProperty().fileValue(getBuildDir(project).toPath().resolve(EMPTY_DIR).toFile()))
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(mainModelsGenDir))
            options.compilerArgs += ['-proc:only', '-A' + COGENGEN_PHASE_KEY + '=' + MODELS]
        }

        project.tasks.named('compileJava', JavaCompile).configure {
            dependsOn 'codeGenVajramModels'

            options.compilerArgs += [

                    // So that vajram impls are generated during compilation
                    '-A' + COGENGEN_PHASE_KEY + '=' + IMPLS,

                    // So that @Resolver method param names can be read at runtime
                    // in case @Using annotation has not been used on the parameters
                    // See VajramDefinition#parseInputResolvers
                    '-parameters']
        }

        project.tasks.register('testCodeGenVajramModels', JavaCompile) {
            //Compile the generatedCode
            group = 'krystal'
            mustRunAfter it.project.tasks.compileJava
            source mainSrcDir, testSrcDir
            classpath = project.configurations.compileClasspath + project.configurations.testCompileClasspath
            // This is a 'proc:only' compile step. Which means, no .class files are generated. This means the destinationDirectory property
            // is not essential used by this step.
            // We reassign the `destinationDirectory` to a dummy empty directory so that the destination directory doesnot clash
            // with the full compile step. This is so that gradle caching works optimally - gradle doesn't cache outputs of tasks
            // which share output directories with other tasks -
            // See: https://docs.gradle.org/current/userguide/build_cache_concepts.html#concepts_overlapping_outputs
            getDestinationDirectory().set(project.getObjects().directoryProperty().fileValue(getBuildDir(project).toPath().resolve(EMPTY_DIR).toFile()))
            //For lombok processing of EqualsAndHashCode
            options.annotationProcessorPath = project.tasks.compileTestJava.options.annotationProcessorPath
            options.generatedSourceOutputDirectory.fileValue(project.file(testModelsGenDir))
            options.compilerArgs += ['-proc:only', '-A' + COGENGEN_PHASE_KEY + '=' + MODELS]
        }

        project.tasks.named("jar").configure { it.dependsOn("compileJava") }
    }

    private static File getBuildDir(Project project) {
        project.getLayout().getBuildDirectory().getAsFile().get()
    }
}
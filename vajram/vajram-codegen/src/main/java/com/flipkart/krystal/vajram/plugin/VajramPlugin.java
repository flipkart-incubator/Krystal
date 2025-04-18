package com.flipkart.krystal.vajram.plugin;

import static com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.VAJRAM_MODELS_GEN_DIR_NAME;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;

public class VajramPlugin implements Plugin<Project> {
  static final String EMPTY_DIR = "tmp/empty";

  static final String SRC_GEN_DIR = "/generated/sources/annotationProcessor/java";
  // Vajram models are generated in a different directory to keep the build phase inputs and
  // outputs separate -
  // This enables better caching and to deterministically reuse previous build's outputs
  static final String VAJRAM_MODELS_GEN_DIR =
      "/generated/sources/" + VAJRAM_MODELS_GEN_DIR_NAME + "/java";

  static final String MAIN_SRC_DIR = "src/main/java";
  static final String TEST_SRC_DIR = "src/test/java";

  @Override
  public void apply(Project project) {

    var mainModelsGenDir =
        new File(getBuildDir(project).getPath() + VAJRAM_MODELS_GEN_DIR + "/main");
    var mainImplsGenDir = new File(getBuildDir(project).getPath() + SRC_GEN_DIR + "/main");

    var testModelsGenDir =
        new File(getBuildDir(project).getPath() + VAJRAM_MODELS_GEN_DIR + "/test");
    var testImplsGenDir = new File(getBuildDir(project).getPath() + SRC_GEN_DIR + "/test");

    addSourceSets(project, mainModelsGenDir, mainImplsGenDir, testModelsGenDir, testImplsGenDir);
    registerCodeGenVajramModels(project, mainModelsGenDir);
    configureCompileJava(project);
    registerTestCodeGenVajramModels(project, testModelsGenDir);

    project.getTasks().named("jar").configure(task -> task.dependsOn("compileJava"));
  }

  private static void addSourceSets(
      Project project,
      File mainModelsGenDir,
      File mainImplsGenDir,
      File testModelsGenDir,
      File testImplsGenDir) {
    SourceSetContainer sourceSets =
        checkNotNull(project.getExtensions().findByType(JavaPluginExtension.class)).getSourceSets();
    sourceSets.getByName("main").getJava().srcDir(mainModelsGenDir).srcDir(mainImplsGenDir);
    sourceSets.getByName("test").getJava().srcDir(testModelsGenDir).srcDir(testImplsGenDir);
  }

  private static void registerCodeGenVajramModels(Project project, File mainModelsGenDir) {
    project
        .getTasks()
        .register(
            "codeGenVajramModels",
            JavaCompile.class,
            task -> {
              //              task.dependsOn("codeGenVajramSchemas");

              // Compile the generatedCode
              task.setGroup("krystal");
              task.source(VajramPlugin.MAIN_SRC_DIR);
              task.setClasspath(project.getConfigurations().getByName("compileClasspath"));
              // This is a 'proc:only' compile step. Which means, no .class files are generated.
              // This means the destinationDirectory property
              // is not used by this step.
              // We reassign the `destinationDirectory` to a dummy empty directory so that the
              // destination directory does not clash
              // with the full compile step. This is so that gradle caching works optimally - gradle
              // doesn't cache outputs of tasks
              // which share output directories with other tasks -
              // See:
              // https://docs.gradle.org/current/userguide/build_cache_concepts.html#concepts_overlapping_outputs
              task.getDestinationDirectory()
                  .set(
                      project
                          .getObjects()
                          .directoryProperty()
                          .fileValue(getBuildDir(project).toPath().resolve(EMPTY_DIR).toFile()));
              // For lombok processing of EqualsAndHashCode
              task.getOptions()
                  .setAnnotationProcessorPath(
                      project
                          .getTasks()
                          .named("compileJava", JavaCompile.class)
                          .get()
                          .getOptions()
                          .getAnnotationProcessorPath());
              task.getOptions()
                  .getGeneratedSourceOutputDirectory()
                  .fileValue(project.file(mainModelsGenDir));
              task.getOptions()
                  .getCompilerArgs()
                  .addAll(List.of("-proc:only", "-A" + CODEGEN_PHASE_KEY + '=' + MODELS));
            });
  }

  private static void configureCompileJava(Project project) {
    project
        .getTasks()
        .named("compileJava", JavaCompile.class)
        .configure(
            task -> {
              task.dependsOn("codeGenVajramModels");

              task.getOptions()
                  .getCompilerArgs()
                  .addAll(
                      List.of(

                          // So that vajram wrappers are generated during compilation
                          "-A" + CODEGEN_PHASE_KEY + '=' + FINAL,

                          // So that @Resolver method param names can be read at runtime
                          // in case @Using annotation has not been used on the parameters
                          // See VajramDefinition#parseInputResolvers
                          "-parameters"));
            });
  }

  private static void registerTestCodeGenVajramModels(Project project, File testModelsGenDir) {
    project
        .getTasks()
        .register(
            "testCodeGenVajramModels",
            JavaCompile.class,
            task -> {
              task.setGroup("krystal");
              task.mustRunAfter(task.getProject().getTasks().getByName("compileJava"));
              task.source(MAIN_SRC_DIR, TEST_SRC_DIR);
              task.setClasspath(
                  project
                      .getConfigurations()
                      .getByName("compileClasspath")
                      .plus(project.getConfigurations().getByName("testCompileClasspath")));
              // This is a 'proc:only' compile step. Which means, no .class files are generated.
              // This means the destinationDirectory property
              // is not essential used by this step.
              // We reassign the `destinationDirectory` to a dummy empty directory so that the
              // destination directory does not clash
              // with the full compile step. This is so that gradle caching works optimally - gradle
              // doesn't cache outputs of tasks
              // which share output directories with other tasks -
              // See:
              // https://docs.gradle.org/current/userguide/build_cache_concepts.html#concepts_overlapping_outputs
              task.getDestinationDirectory()
                  .set(
                      project
                          .getObjects()
                          .directoryProperty()
                          .fileValue(getBuildDir(project).toPath().resolve(EMPTY_DIR).toFile()));
              // For lombok processing of EqualsAndHashCode
              task.getOptions()
                  .setAnnotationProcessorPath(
                      project
                          .getTasks()
                          .named("compileTestJava", JavaCompile.class)
                          .get()
                          .getOptions()
                          .getAnnotationProcessorPath());
              task.getOptions()
                  .getGeneratedSourceOutputDirectory()
                  .fileValue(project.file(testModelsGenDir));
              task.getOptions()
                  .getCompilerArgs()
                  .addAll(List.of("-proc:only", "-A" + CODEGEN_PHASE_KEY + '=' + MODELS));
            });
  }

  private static File getBuildDir(Project project) {
    return project.getLayout().getBuildDirectory().getAsFile().get();
  }
}

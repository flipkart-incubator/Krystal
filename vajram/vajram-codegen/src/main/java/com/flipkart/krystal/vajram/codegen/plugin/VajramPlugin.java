package com.flipkart.krystal.vajram.codegen.plugin;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.FINAL;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.CODEGEN_PHASE_KEY;
import static com.flipkart.krystal.config.PropertyNames.RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.VAJRAM_MODELS_GEN_DIR_NAME;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyScopeConfiguration;
import org.gradle.api.artifacts.ResolvableConfiguration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;

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
  public static final String KRYSTAL_MODELS_GEN = "krystalModelsGen";
  public static final String KRYSTAL_MODELS_GEN_PROC = KRYSTAL_MODELS_GEN + "Processor";
  public static final String KRYSTAL_MODELS_GEN_PROC_PATH = KRYSTAL_MODELS_GEN_PROC + "Path";

  @Override
  public void apply(Project project) {
    var mainModelsGenDir =
        new File(getBuildDir(project).getPath() + VAJRAM_MODELS_GEN_DIR + "/main");
    var mainImplsGenDir = new File(getBuildDir(project).getPath() + SRC_GEN_DIR + "/main");

    var testModelsGenDir =
        new File(getBuildDir(project).getPath() + VAJRAM_MODELS_GEN_DIR + "/test");
    var testImplsGenDir = new File(getBuildDir(project).getPath() + SRC_GEN_DIR + "/test");

    addSourceSets(project, mainModelsGenDir, mainImplsGenDir, testModelsGenDir, testImplsGenDir);
    registerKrystalModelsGen(project, mainModelsGenDir);
    configureCompileJava(project);
    registerTestKrystalModelsGen(project, testModelsGenDir);

    project.getTasks().named("jar").configure(task -> task.dependsOn("compileJava"));

    //noinspection TestOnlyProblems
    project
        .getTasks()
        .withType(Test.class)
        .named("test")
        .configure(
            task ->
                task.systemProperty(RISKY_OPEN_ALL_VAJRAMS_TO_EXTERNAL_INVOCATION_PROP_NAME, true));
  }

  private static void addSourceSets(
      Project project,
      File mainModelsGenDir,
      File mainImplsGenDir,
      File testModelsGenDir,
      File testImplsGenDir) {
    JavaPluginExtension javaPluginExtension =
        project.getExtensions().findByType(JavaPluginExtension.class);
    if (javaPluginExtension == null) {
      return;
    }
    SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
    sourceSets.getByName("main").getJava().srcDir(mainModelsGenDir).srcDir(mainImplsGenDir);
    sourceSets.getByName("test").getJava().srcDir(testModelsGenDir).srcDir(testImplsGenDir);
  }

  private static void registerKrystalModelsGen(Project project, File mainModelsGenDir) {
    @SuppressWarnings("UnstableApiUsage")
    DependencyScopeConfiguration krystalModelsGenProcessor =
        project.getConfigurations().dependencyScope(KRYSTAL_MODELS_GEN_PROC).get();

    @SuppressWarnings("UnstableApiUsage")
    ResolvableConfiguration krystalModelsGenProcessorPath =
        project
            .getConfigurations()
            .resolvable(KRYSTAL_MODELS_GEN_PROC_PATH, f -> f.extendsFrom(krystalModelsGenProcessor))
            .get();

    project
        .getConfigurations()
        .getByName(
            "annotationProcessor",
            annotationProcessor -> annotationProcessor.extendsFrom(krystalModelsGenProcessorPath));
    project
        .getTasks()
        .register(
            KRYSTAL_MODELS_GEN,
            JavaCompile.class,
            task -> {
              // Compile the generatedCode
              task.setGroup("krystal");
              task.source(MAIN_SRC_DIR);
              task.setClasspath(project.getConfigurations().getByName("compileClasspath"));
              // This is a 'proc:only' compile step. Which means, no .class files are generated.
              // This means the destinationDirectory property
              // is not used by this step.
              // We reassign the `destinationDirectory` to a dummy empty directory so that the
              // destination directory does not clash
              // with the full compile step. This is so that gradle caching works optimally -
              // gradle doesn't cache outputs of tasks which share output directories with other
              // tasks -
              // See:
              // https://docs.gradle.org/current/userguide/build_cache_concepts.html#concepts_overlapping_outputs
              task.getDestinationDirectory()
                  .set(
                      project
                          .getObjects()
                          .directoryProperty()
                          .fileValue(getBuildDir(project).toPath().resolve(EMPTY_DIR).toFile()));
              task.getOptions()
                  .getGeneratedSourceOutputDirectory()
                  .fileValue(project.file(mainModelsGenDir));
              task.getOptions()
                  .getCompilerArgs()
                  .addAll(List.of("-proc:only", "-A" + CODEGEN_PHASE_KEY + '=' + MODELS));
            })
        .configure(
            krystalModelsGen ->
                krystalModelsGen.doFirst(
                    _t ->
                        krystalModelsGen
                            .getOptions()
                            .setAnnotationProcessorPath(
                                project
                                    .getConfigurations()
                                    .named(KRYSTAL_MODELS_GEN_PROC_PATH)
                                    .get())));
  }

  private static void configureCompileJava(Project project) {
    project
        .getTasks()
        .named("compileJava", JavaCompile.class)
        .configure(
            task -> {
              task.dependsOn(KRYSTAL_MODELS_GEN);

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

  private static void registerTestKrystalModelsGen(Project project, File testModelsGenDir) {
    @SuppressWarnings("UnstableApiUsage")
    DependencyScopeConfiguration testKrystalModelsGenProcessor =
        project
            .getConfigurations()
            .dependencyScope("test" + capitalizeFirstChar(KRYSTAL_MODELS_GEN_PROC))
            .get();

    String testModelGenProcessorPath = "test" + capitalizeFirstChar(KRYSTAL_MODELS_GEN_PROC_PATH);

    @SuppressWarnings("UnstableApiUsage")
    ResolvableConfiguration testKrystalModelsGenProcessorPath =
        project
            .getConfigurations()
            .resolvable(
                testModelGenProcessorPath, f -> f.extendsFrom(testKrystalModelsGenProcessor))
            .get();

    project
        .getConfigurations()
        .getByName(
            "testAnnotationProcessor",
            annotationProcessor ->
                annotationProcessor.extendsFrom(testKrystalModelsGenProcessorPath));
    project
        .getTasks()
        .register(
            "test" + capitalizeFirstChar(KRYSTAL_MODELS_GEN),
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
              task.getOptions()
                  .getGeneratedSourceOutputDirectory()
                  .fileValue(project.file(testModelsGenDir));
              task.getOptions()
                  .getCompilerArgs()
                  .addAll(List.of("-proc:only", "-A" + CODEGEN_PHASE_KEY + '=' + MODELS));
            })
        .configure(
            testKrystalModelsGen ->
                testKrystalModelsGen.doFirst(
                    _t ->
                        testKrystalModelsGen
                            .getOptions()
                            .setAnnotationProcessorPath(
                                project
                                    .getConfigurations()
                                    .named(testModelGenProcessorPath)
                                    .get())));
  }

  private static @NonNull String capitalizeFirstChar(String krystalModelsGen) {
    return krystalModelsGen.substring(0, 1).toUpperCase() + krystalModelsGen.substring(1);
  }

  private static File getBuildDir(Project project) {
    return project.getLayout().getBuildDirectory().getAsFile().get();
  }
}

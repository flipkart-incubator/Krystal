package com.flipkart.krystal.vajram.codegen.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.codegen.common.processors.ModelGenProcessor;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Verifies (via in-process annotation processing) the compile-time validations of field and
 * batching facets. Qualified facet names are written as string literals since generated {@code
 * _Fac} constants are not available in this phase.
 */
class FacetValidationTest {

  private static final String HEADER =
      """
      package fixtures;

      import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

      import com.flipkart.krystal.model.IfAbsent;
      import com.flipkart.krystal.vajram.ComputeVajramDef;
      import com.flipkart.krystal.vajram.Vajram;
      import com.flipkart.krystal.vajram.batching.Batched;
      import com.flipkart.krystal.vajram.batching.BatchesGroupedBy;
      import com.flipkart.krystal.vajram.facets.Field;
      import com.flipkart.krystal.vajram.facets.Output;
      import com.flipkart.krystal.vajram.facets.ComputeField;

      """;

  @Test
  void fieldWithoutComputeMethod_isRejected() {
    List<String> errors =
        compileErrors(
            "MissingComputeVajram",
            """
            @Vajram
            public abstract class MissingComputeVajram extends ComputeVajramDef<String> {
              interface _Inputs {
                @IfAbsent(FAIL) String name();
              }
              interface _InternalFacets {
                @IfAbsent(FAIL) @Field String greeting();
              }
              @Output
              static String output(String greeting) { return greeting; }
            }
            """);

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "Field facet 'greeting' is declared but has no corresponding @ComputeField"));
  }

  @Test
  void computeFieldTargetingNonField_isRejected() {
    List<String> errors =
        compileErrors(
            "NonFieldTargetVajram",
            """
            @Vajram
            public abstract class NonFieldTargetVajram extends ComputeVajramDef<String> {
              interface _Inputs {
                @IfAbsent(FAIL) String name();
              }
              @ComputeField("NonFieldTargetVajram:name")
              static String computeName() { return "x"; }
              @Output
              static String output(String name) { return name; }
            }
            """);

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "@ComputeField target 'name' is not a @Field facet declared in _InternalFacets"));
  }

  @Test
  void duplicateComputeFieldMethods_areRejected() {
    List<String> errors =
        compileErrors(
            "DuplicateComputeVajram",
            """
            @Vajram
            public abstract class DuplicateComputeVajram extends ComputeVajramDef<String> {
              interface _Inputs {
                @IfAbsent(FAIL) String name();
              }
              interface _InternalFacets {
                @IfAbsent(FAIL) @Field String greeting();
              }
              @ComputeField("DuplicateComputeVajram:greeting")
              static String first(String name) { return "Hi " + name; }
              @ComputeField("DuplicateComputeVajram:greeting")
              static String second(String name) { return "Hello " + name; }
              @Output
              static String output(String greeting) { return greeting; }
            }
            """);

    assertThat(errors)
        .anyMatch(e -> e.contains("Duplicate @ComputeField methods for field 'greeting'"));
  }

  @Test
  void cyclicFields_areRejectedWithCyclePath() {
    List<String> errors =
        compileErrors(
            "CyclicFieldsVajram",
            """
            @Vajram
            public abstract class CyclicFieldsVajram extends ComputeVajramDef<String> {
              interface _Inputs {
                @IfAbsent(FAIL) String name();
              }
              interface _InternalFacets {
                @IfAbsent(FAIL) @Field String a();
                @IfAbsent(FAIL) @Field String b();
              }
              @ComputeField("CyclicFieldsVajram:a")
              static String computeA(String b) { return b; }
              @ComputeField("CyclicFieldsVajram:b")
              static String computeB(String a) { return a; }
              @Output
              static String output(String a) { return a; }
            }
            """);

    assertThat(errors)
        .anyMatch(e -> e.contains("Cycle detected in facet dependencies") && e.contains("→"));
  }

  @Test
  void batchesGroupedByOnNonGivenFacet_isRejected() {
    List<String> errors =
        compileErrors(
            "GroupedByFieldVajram",
            batchedVajram(
                "GroupedByFieldVajram",
                "@IfAbsent(FAIL) @Batched String id();",
                "@IfAbsent(FAIL) @BatchesGroupedBy @Field String key();",
                """
                @ComputeField("GroupedByFieldVajram:key")
                static String computeKey() { return "k"; }
                """));

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "@BatchesGroupedBy can be placed only on input and injection facets. 'key' is a"
                        + " FIELD facet"));
  }

  @Test
  void batchesGroupedByOnDependency_isRejected() {
    List<String> errors =
        compileErrors(
            Map.of(
                "GroupedByDependencyVajram",
                batchedVajram(
                    "GroupedByDependencyVajram",
                    "@IfAbsent(FAIL) @Batched String id();",
                    """
                    @IfAbsent(FAIL) @BatchesGroupedBy
                    @com.flipkart.krystal.vajram.facets.Dependency(onVajram = DependencyTargetVajram.class)
                    int lookup();
                    """,
                    """
                    @com.flipkart.krystal.vajram.facets.resolution.Resolve(
                        dep = "GroupedByDependencyVajram:lookup",
                        depInputs = "DependencyTargetVajram:n")
                    static int resolveN() { return 1; }
                    """),
                "DependencyTargetVajram",
                """
                @Vajram
                public abstract class DependencyTargetVajram extends ComputeVajramDef<Integer> {
                  interface _Inputs {
                    @IfAbsent(FAIL) int n();
                  }
                  @Output
                  static int output(int n) { return n; }
                }
                """));

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "@BatchesGroupedBy can be placed only on input and injection facets. 'lookup'"
                        + " is a DEPENDENCY facet"));
  }

  @Test
  void batchedAndBatchesGroupedByOnSameFacet_isRejected() {
    List<String> errors =
        compileErrors(
            "BothAnnotationsVajram",
            batchedVajram(
                "BothAnnotationsVajram",
                "@IfAbsent(FAIL) @Batched @BatchesGroupedBy String id();",
                "",
                ""));

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "Facet 'id' cannot be annotated with both @Batched and @BatchesGroupedBy"));
  }

  @Test
  void givenFacetWithoutBatchingAnnotation_inBatchedVajram_isRejected() {
    List<String> errors =
        compileErrors(
            "UnannotatedInputVajram",
            batchedVajram(
                "UnannotatedInputVajram",
                "@IfAbsent(FAIL) @Batched String id(); @IfAbsent(FAIL) String tenant();",
                "",
                ""));

    assertThat(errors)
        .anyMatch(
            e ->
                e.contains(
                    "Facet 'tenant' of a vajram which supports batching must be annotated with"
                        + " either @Batched or @BatchesGroupedBy"));
  }

  @Test
  void nonBatchedField_dependingOnBatchedInput_isRejected() {
    List<String> errors =
        compileErrors(
            "NonBatchedFieldVajram",
            batchedVajram(
                "NonBatchedFieldVajram",
                "@IfAbsent(FAIL) @Batched String id();",
                "@IfAbsent(FAIL) @Field String key();",
                """
                @ComputeField("NonBatchedFieldVajram:key")
                static String computeKey(String id) { return id; }
                """));

    assertThat(errors)
        .anyMatch(e -> e.contains("Facet 'key' cannot depend on @Batched facet 'id'"));
  }

  @Test
  void validBatchedVajram_producesNoBatchingErrors() {
    List<String> errors =
        compileErrors(
            "ValidBatchedVajram",
            batchedVajram(
                "ValidBatchedVajram",
                "@IfAbsent(FAIL) @Batched String id(); @IfAbsent(FAIL) @BatchesGroupedBy String"
                    + " tenant();",
                "@IfAbsent(FAIL) @Field String key();",
                """
                @ComputeField("ValidBatchedVajram:key")
                static String computeKey(String tenant) { return tenant; }
                """));

    assertThat(errors)
        .noneMatch(
            e ->
                e.contains("@BatchesGroupedBy can be placed only")
                    || e.contains("cannot be annotated with both")
                    || e.contains("must be annotated with either")
                    || e.contains("cannot depend on @Batched facet"));
  }

  /** A vajram which supports batching (has at least one {@code @Batched} facet). */
  private static String batchedVajram(
      String name, String inputs, String internalFacets, String extraMethods) {
    return """
        @Vajram
        public abstract class %1$s extends com.flipkart.krystal.vajram.IOVajramDef<String> {
          interface _Inputs { %2$s }
          interface _InternalFacets { %3$s }
          %4$s
          @Output.Batched
          static java.util.concurrent.CompletableFuture<String> batchedOutput(
              java.util.Collection<%1$s_BatchItem> _batchItems) {
            return java.util.concurrent.CompletableFuture.completedFuture("");
          }
          @Output.Unbatch
          static java.util.Map<%1$s_BatchItem, com.flipkart.krystal.data.Errable<String>> unbatch(
              java.util.Collection<%1$s_BatchItem> _batchItems,
              com.flipkart.krystal.data.Errable<String> _batchedOutput) {
            return java.util.Map.of();
          }
        }
        """
        .formatted(name, inputs, internalFacets, extraMethods);
  }

  @Test
  void validChainedFields_produceNoFieldValidationErrors() {
    List<String> errors =
        compileErrors(
            "ValidFieldsVajram",
            """
            @Vajram
            public abstract class ValidFieldsVajram extends ComputeVajramDef<String> {
              interface _Inputs {
                @IfAbsent(FAIL) String name();
              }
              interface _InternalFacets {
                @IfAbsent(FAIL) @Field String a();
                @IfAbsent(FAIL) @Field String b();
              }
              @ComputeField("ValidFieldsVajram:a")
              static String computeA(String name) { return name; }
              @ComputeField("ValidFieldsVajram:b")
              static String computeB(String a) { return a; }
              @Output
              static String output(String b) { return b; }
            }
            """);

    assertThat(errors)
        .noneMatch(
            e ->
                e.contains("Cycle detected")
                    || e.contains("Duplicate @ComputeField")
                    || e.contains("no corresponding @ComputeField")
                    || e.contains("@BatchesGroupedBy facet"));
  }

  /**
   * Mirrors the real build: first the MODELS phase generates the request/facet models into a temp
   * dir, then the FINAL phase processes the vajram together with the generated sources. Only the
   * FINAL phase diagnostics are returned since that is where field validations run.
   */
  private static List<String> compileErrors(String typeName, String body) {
    return compileErrors(Map.of(typeName, body));
  }

  /** The keys of {@code sources} are type names and the values are the bodies of the types. */
  private static List<String> compileErrors(Map<String, String> sources) {
    try {
      Path modelsDir = Files.createTempDirectory("field-facet-validation-models");
      modelsDir.toFile().deleteOnExit();
      List<JavaFileObject> fixtures =
          sources.entrySet().stream()
              .<JavaFileObject>map(
                  e -> new StringSourceFile("fixtures." + e.getKey(), HEADER + e.getValue()))
              .toList();
      runProcessors(
          "MODELS",
          List.of(new ModelGenProcessor(), new VajramModelGenProcessor()),
          modelsDir,
          fixtures);

      Path finalDir = Files.createTempDirectory("field-facet-validation-final");
      finalDir.toFile().deleteOnExit();
      List<JavaFileObject> finalSources = new ArrayList<>(fixtures);
      try (Stream<Path> generated = Files.walk(modelsDir)) {
        generated
            .filter(f -> f.toString().endsWith(".java"))
            .forEach(f -> finalSources.add(new PathSourceFile(f)));
      }
      return runProcessors(
              "FINAL", List.of(new VajramWrapperGenProcessor()), finalDir, finalSources)
          .stream()
          .filter(d -> d.getKind() == Kind.ERROR)
          .map(d -> d.getMessage(null))
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static List<Diagnostic<? extends JavaFileObject>> runProcessors(
      String phase, List<Processor> processors, Path outputDir, List<JavaFileObject> sources) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaCompiler.CompilationTask task =
        compiler.getTask(
            new StringWriter(),
            null,
            diagnostics,
            List.of(
                "-proc:only",
                "-classpath",
                System.getProperty("java.class.path"),
                "-s",
                outputDir.toString(),
                "-Akrystal.codegen.phase=" + phase),
            null,
            sources);
    task.setProcessors(processors);
    task.call();
    return diagnostics.getDiagnostics();
  }

  private static final class PathSourceFile extends SimpleJavaFileObject {
    private final Path path;

    PathSourceFile(Path path) {
      super(path.toUri(), Kind.SOURCE);
      this.path = path;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
      return Files.readString(path);
    }
  }

  private static final class StringSourceFile extends SimpleJavaFileObject {
    private final String content;

    StringSourceFile(String qualifiedName, String content) {
      super(
          URI.create("string:///" + qualifiedName.replace('.', '/') + Kind.SOURCE.extension),
          Kind.SOURCE);
      this.content = content;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return content;
    }
  }
}

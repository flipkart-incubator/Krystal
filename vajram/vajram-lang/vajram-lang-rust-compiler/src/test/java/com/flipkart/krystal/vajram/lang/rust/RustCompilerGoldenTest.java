package com.flipkart.krystal.vajram.lang.rust;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.lang.rust.ast.Callers.Caller;
import com.flipkart.krystal.vajram.lang.rust.cli.RustCompilerMain;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Compiles every {@code .vajram} fixture (copied from {@code vajram-lang-grammar}'s known-passing
 * parser test resources) and checks the generated Rust against a checked-in "golden" copy under
 * {@code src/test/resources/expected/}.
 *
 * <p>If a golden file doesn't exist yet, this test writes one and fails with instructions to review
 * and commit it - the standard low-maintenance way to bootstrap/update golden tests without
 * hand-authoring expected Rust text.
 */
class RustCompilerGoldenTest {

  private static final Path FIXTURES_DIR = Path.of("src/test/resources/vajram");
  private static final Path EXPECTED_DIR = Path.of("src/test/resources/expected");
  private static final boolean UPDATE_GOLDENS = Boolean.getBoolean("updateRustGoldens");

  @Test
  void compilesAllFixturesWithoutErrors(@TempDir Path outDir) throws IOException {
    boolean ok = RustCompilerMain.compile(FIXTURES_DIR, outDir);
    assertThat(ok).as("compilation should succeed with no diagnostic errors").isTrue();
  }

  @Test
  void generatedRustMatchesGoldenFiles(@TempDir Path outDir) throws IOException {
    boolean ok = RustCompilerMain.compile(FIXTURES_DIR, outDir);
    assertThat(ok).isTrue();

    List<Path> generated = listRsFiles(outDir);
    List<String> missingGolden = new java.util.ArrayList<>();
    List<String> mismatched = new java.util.ArrayList<>();
    for (Path file : generated) {
      Path relative = outDir.relativize(file);
      Path golden = EXPECTED_DIR.resolve(relative);
      String actual = Files.readString(file);
      if (!Files.exists(golden)) {
        Files.createDirectories(golden.getParent());
        Files.writeString(golden, actual);
        missingGolden.add(relative.toString());
        continue;
      }
      String expected = Files.readString(golden);
      if (!expected.equals(actual)) {
        if (UPDATE_GOLDENS) {
          Files.writeString(golden, actual);
        } else {
          mismatched.add(relative.toString());
        }
      }
    }
    if (!missingGolden.isEmpty() && !UPDATE_GOLDENS) {
      throw new AssertionError(
          "Wrote new golden file(s), review and commit them: " + missingGolden);
    }
    assertThat(mismatched).as("generated Rust drifted from golden files").isEmpty();
  }

  @Test
  void emittedRustPassesRustcSyntaxCheckWhenAvailable(@TempDir Path tempDir) throws Exception {
    Assumptions.assumeTrue(commandAvailable("rustc"), "rustc is not available on PATH");
    Assumptions.assumeTrue(supportsRust2024(), "rustc does not support the Rust 2024 edition");
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.vajram"),
        """
        package smoke;
        vajram hello() out string {
          { "hello" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    Process process =
        new ProcessBuilder(
                "rustc",
                "--edition",
                "2024",
                "--crate-type",
                "lib",
                "lib.rs",
                "--out-dir",
                outDir.resolve("target").toString())
            .directory(outDir.toFile())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    assertThat(process.waitFor()).as(output).isZero();
  }

  @Test
  void permitsCrossCompletionDependenciesAndDefersAsyncDependencyUse(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("source");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("source.vajram"),
        """
        package lifecycle;
        vajram laterLeaf() out string { ~~ { "leaf" } }
        """);
    Files.writeString(
        sourceDir.resolve("caller.vajram"),
        """
        package lifecycle;
        vajram nowCaller() out string {
          string value = laterLeaf();
          { value }
        }
        """);
    Path outDir = tempDir.resolve("out");
    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String caller = Files.readString(outDir.resolve("lifecycle/caller.rs"));
    assertThat(caller).contains("pub async fn call");
    assertThat(caller).contains("crate::vajram_rt::spawn_local_shared(async move");
    assertThat(caller).contains("value.clone().await");
  }

  @Test
  void outsideProcessProcessorGeneratesAggregatingEntrypoint(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("first.vajram"),
        """
        package external;
        vajram first(string input) out string permit callers `outsideProcess public {
          { input }
        }
        """);
    Files.writeString(
        sourceDir.resolve("second.vajram"),
        """
        package external;
        vajram second() out string permit callers `outsideProcess public {
          { "second" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String main = Files.readString(outDir.resolve("main.rs"));
    assertThat(main).contains("match vajram.as_str()");
    assertThat(main).contains("usage: <program> <vajram-name> [--input value]...");
    assertThat(main).contains(".strip_prefix(\"--\")");
    assertThat(main).contains(".get(\"input\")");
    assertThat(main).contains("\"first\" =>");
    assertThat(main).contains("\"second\" =>");
    assertThat(main).contains("external::first::first::call(");
    assertThat(main).contains("FirstInputs {");
    assertThat(main)
        .contains("external::second::second::call(external::second::second::SecondInputs {})");
  }

  @Test
  void preservesAnnotationsForEachNamedCaller() {
    var diagnostics = new com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.vajram.lang.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("callers.vajram"),
                """
                package test;
                vajram target() out void permit callers `first callerA, `second callerB {
                  { }
                }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var file = parsed.orElseThrow();
    assertThat(file.vajram().callers())
        .isInstanceOf(com.flipkart.krystal.vajram.lang.rust.ast.Callers.Named.class);
    var named = (com.flipkart.krystal.vajram.lang.rust.ast.Callers.Named) file.vajram().callers();
    assertThat(named.callers())
        .containsExactly(
            new Caller(List.of("first"), "callerA"), new Caller(List.of("second"), "callerB"));
  }

  @Test
  void parsesOrderedFieldsAndArrayExpressions() {
    var diagnostics = new com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.vajram.lang.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("fields.vajram"),
                """
                package test;
                vajram fields() out void {
                  string values = ["one", "two"];
                  { }
                }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var file = parsed.orElseThrow();
    assertThat(file.vajram().computedFacets())
        .singleElement()
        .isInstanceOf(com.flipkart.krystal.vajram.lang.rust.ast.Field.class);
    var field =
        (com.flipkart.krystal.vajram.lang.rust.ast.Field) file.vajram().computedFacets().get(0);
    assertThat(field.value())
        .isInstanceOf(com.flipkart.krystal.vajram.lang.rust.ast.Expr.Array.class);
  }

  @Test
  void rejectsScalarFanoutFieldsAndMismatchedDependencyCardinality(@TempDir Path tempDir)
      throws IOException {
    Path scalarSource = tempDir.resolve("scalar");
    Files.createDirectories(scalarSource);
    Files.writeString(
        scalarSource.resolve("scalar.vajram"),
        """
        package fanout;
        vajram scalar() out void {
          string* values = "not a collection";
          { }
        }
        """);
    assertThat(RustCompilerMain.compile(scalarSource, tempDir.resolve("scalar-out"))).isFalse();

    Path mismatchSource = tempDir.resolve("mismatch");
    Files.createDirectories(mismatchSource);
    Files.writeString(
        mismatchSource.resolve("leaf.vajram"),
        """
        package fanout;
        vajram leaf() out string { { "leaf" } }
        """);
    Files.writeString(
        mismatchSource.resolve("parent.vajram"),
        """
        package fanout;
        vajram parent() out void {
          string* values = leaf();
          { }
        }
        """);
    assertThat(RustCompilerMain.compile(mismatchSource, tempDir.resolve("mismatch-out"))).isFalse();
  }

  @Test
  void lowersReadFileAsStringToTokioUtf8Io(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("reader.vajram"),
        """
        package system;
        import vajram readFileAsString from lang.FileSystem;
        vajram reader(string filePath) out string~ permit callers public {
          string content = readFileAsString(path = filePath);
          out ~ { content }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String reader = Files.readString(outDir.resolve("system/reader.rs"));
    assertThat(reader).contains("tokio::fs::read_to_string(_content_inputs.filePath.as_str())");
    assertThat(reader).contains("crate::vajram_rt::spawn_local_shared(async move");
  }

  @Test
  void startsIndependentFacetsBeforeTheirComputedFieldTasks(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("graph.vajram"),
        """
        package graph;
        vajram firstLeaf() out string { ~ { "first" } }
        vajram secondLeaf() out string { ~ { "second" } }
        vajram parent() out string {
          string first = firstLeaf();
          string second = secondLeaf();
          string firstValue = first + "";
          string secondValue = second + "";
          out ~ { firstValue }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String parent = Files.readString(outDir.resolve("graph/graph.rs"));
    assertThat(parent).contains("let first = crate::vajram_rt::spawn_local_shared");
    assertThat(parent).contains("let second = crate::vajram_rt::spawn_local_shared");
    assertThat(parent).contains("let firstValue = crate::vajram_rt::spawn_local_shared");
    assertThat(parent).contains("let secondValue = crate::vajram_rt::spawn_local_shared");
    assertThat(parent.indexOf("let second =")).isLessThan(parent.indexOf("first.clone().await"));
  }

  @Test
  void lowersConcatStringsToRustStringJoin(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("join.vajram"),
        """
        package system;
        import vajram concatStrings from lang.Strings;
        vajram join(string separator) out string {
          string values = ["one", "two"];
          out concatStrings(strings = values; separator = separator);
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String join = Files.readString(outDir.resolve("system/join.rs"));
    assertThat(join)
        .contains("values")
        .contains("value.as_str()")
        .contains("collect::<Vec<_>>()")
        .contains("join(inputs.separator.as_str())");
  }

  @Test
  void emitsOneRustFileForMultipleVajramsInOneSourceFile(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("combined.vajram"),
        """
        package combined;
        vajram leaf() out string { { "leaf" } }

        vajram caller() out string {
          string value = leaf();
          { value }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("combined/mod.rs"))).contains("pub mod combined;");
    assertThat(Files.exists(outDir.resolve("combined/leaf.rs"))).isFalse();
    assertThat(Files.exists(outDir.resolve("combined/caller.rs"))).isFalse();
    assertThat(Files.readString(outDir.resolve("combined/combined.rs")))
        .contains("pub struct LeafInputs")
        .contains("pub struct CallerInputs")
        .contains(
            "crate::combined::combined::leaf::call(crate::combined::combined::leaf::LeafInputs {})");
  }

  private static boolean commandAvailable(String command) {
    try {
      Process process = new ProcessBuilder(command, "--version").start();
      return process.waitFor() == 0;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static boolean supportsRust2024() {
    try {
      Process process = new ProcessBuilder("rustc", "--version").redirectErrorStream(true).start();
      String version = new String(process.getInputStream().readAllBytes());
      if (process.waitFor() != 0) {
        return false;
      }
      String[] parts = version.split("\\s+");
      String[] numbers = parts.length > 1 ? parts[1].split("\\.") : new String[0];
      return numbers.length >= 2
          && Integer.parseInt(numbers[0]) == 1
          && Integer.parseInt(numbers[1]) >= 85;
    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static List<Path> listRsFiles(Path dir) throws IOException {
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths
          .filter(p -> p.toString().endsWith(".rs"))
          .sorted(Comparator.naturalOrder())
          .toList();
    }
  }
}

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
    // A real `cargo build` (rather than a bare `rustc` invocation) since the generated crate now
    // depends on the `bumpalo` arena crate for its output arena.
    Assumptions.assumeTrue(commandAvailable("cargo"), "cargo is not available on PATH");
    Assumptions.assumeTrue(supportsRust2024(), "rustc does not support the Rust 2024 edition");
    Path cargoDir = tempDir.resolve("cargo");
    Path sourceDir = tempDir.resolve("vajram");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.vajram"),
        """
        package smoke;
        vajram hello(int32 count) out string inject (ConsoleWriter console) {
          { console.println("hello"); "hello" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, cargoDir.resolve("src"))).isTrue();
    Files.writeString(
        cargoDir.resolve("Cargo.toml"),
        """
        [package]
        name = "smoke"
        version = "0.1.0"
        edition = "2024"

        [dependencies]
        futures = "0.3"
        tokio = { version = "1", features = ["rt", "fs"] }
        reqwest = "0.12"
        bumpalo = "3"
        """);

    Process process =
        new ProcessBuilder("cargo", "build")
            .directory(cargoDir.toFile())
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
        vajram laterLeaf() out string~ { ~~ { "leaf" } }
        """);
    Files.writeString(
        sourceDir.resolve("caller.vajram"),
        """
        package lifecycle;
        vajram nowCaller() out string~ {
          string value = laterLeaf();
          { value }
        }
        """);
    Path outDir = tempDir.resolve("out");
    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String caller = Files.readString(outDir.resolve("lifecycle/caller.rs"));
    assertThat(caller).contains("pub async fn call");
    assertThat(caller).contains("let value_fut = async move {");
    assertThat(caller).contains("let value = value_fut.clone().await;");
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
    assertThat(main).contains("AppContext::new");
    assertThat(main).contains("external::second::second::call(").contains("SecondInputs {}");
  }

  @Test
  void wasmTargetGeneratesTypedOutsideProcessDispatchers(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("dispatch.vajram"),
        """
        package external;
        vajram greet(string name, int count) out string permit callers `outsideProcess public {
          { name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    assertThat(Files.exists(outDir.resolve("main.rs"))).isFalse();
    assertThat(Files.readString(outDir.resolve("lib.rs"))).contains("pub mod wasm_dispatch;");
    assertThat(Files.readString(outDir.resolve("wasm_dispatch.rs")))
        .contains("use wasm_bindgen::prelude::*;")
        .contains("#[wasm_bindgen]")
        .contains("pub fn outside_process_greet(name: String, count: i64) -> String")
        .contains("name: arena.alloc(name)")
        .contains("count: arena.alloc(count)");
    assertThat(Files.readString(outDir.resolve("vajram_rt/mod.rs")))
        .contains("WASM prelude")
        .contains("wasm_bindgen_futures::JsFuture")
        .doesNotContain("tokio::")
        .doesNotContain("every generated crate's");
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
  void preservesProviderAnnotationOnVajram() {
    var diagnostics = new com.flipkart.krystal.vajram.lang.rust.diag.Diagnostics();
    var parsed =
        new com.flipkart.krystal.vajram.lang.rust.parse.AstBuilder(diagnostics)
            .build(
                Path.of("provider.vajram"),
                """
                package test;
                `provider(forScope = REQUEST) vajram database() out Database { { new Database() } }
                """);

    assertThat(diagnostics.hasErrors()).as(diagnostics.all().toString()).isFalse();
    var annotation = parsed.orElseThrow().vajram().annotations().get(0);
    assertThat(annotation.name()).isEqualTo("provider");
    assertThat(annotation.arguments())
        .singleElement()
        .satisfies(
            argument -> {
              assertThat(argument.name()).isEqualTo("forScope");
              assertThat(argument.value())
                  .isEqualTo(
                      new com.flipkart.krystal.vajram.lang.rust.ast.Expr.VarUse("REQUEST", false));
            });
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
  void createsSingletonProvidersAndPropagatesContextForInjections(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("injected.vajram"),
        """
        package injections;
        vajram leaf() out string inject (string prefix) {
          { prefix }
        }
        vajram parent() out string {
          string value = leaf();
          { value }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("injections/injected.rs"));
    assertThat(generated)
        .contains("pub struct Leaf_Injections")
        .contains("prefix: Rc<dyn crate::vajram_rt::Provider<String>>")
        .contains("InjectionKey::new(\"string\", &[])")
        .contains("fn instance<I: crate::vajram_rt::Injector + 'static>")
        .contains("let deps = Leaf_Injections::instance(context);")
        .contains("::leaf::call(")
        .contains(", arena, context)")
        .contains("deps.prefix.get()");
  }

  @Test
  void usesFullyQualifiedImportedTypeForInjectionKeys(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("injected.vajram"),
        """
        package injections;
        import type ConsoleWriter from lang.process;
        vajram logger() out void inject (ConsoleWriter writer) {
          { writer.println("hello") }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("injections/injected.rs")))
        .contains("InjectionKey::new(\"lang.process.ConsoleWriter\", &[])");
  }

  @Test
  void emitsBareExpressionStatementsInLogicBlocks(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("logger.vajram"),
        """
        package test;
        vajram logger(string message) out void {
          { Console.log(message); }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("test/logger.rs")))
        .contains("Console.log(inputs.message);");
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
          string... values = "not a collection";
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
          string... values = leaf();
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
        import vajram readFileAsString from lang.fileSystem;
        vajram reader(string filePath) out string~ permit callers public {
          string content = readFileAsString(path = filePath);
          out ~ { content }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String reader = Files.readString(outDir.resolve("system/reader.rs"));
    assertThat(reader).contains("tokio::fs::read_to_string(inputs.filePath.as_str())");
    assertThat(reader).contains("let content_fut = async move {");
  }

  @Test
  void lowersCallHttpToReqwestNonBlockingGet(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fetcher.vajram"),
        """
        package system;
        import vajram callHttp from lang.net;
        vajram fetcher(string requestUrl) out string~ permit callers public {
          string body = callHttp(url = requestUrl);
          out ~ { body }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String fetcher = Files.readString(outDir.resolve("system/fetcher.rs"));
    assertThat(fetcher)
        .contains("reqwest::Client::new()")
        .contains(".get(inputs.requestUrl.as_str())")
        .contains(".header(reqwest::header::USER_AGENT,");
    assertThat(fetcher).contains(".text()").contains(".expect(\"callHttp failed\")");
    assertThat(fetcher).contains("let body_fut = async move {");
  }

  @Test
  void lowersCallHttpToBrowserFetchOnWasmTarget(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fetcher.vajram"),
        """
        package system;
        import vajram callHttp from lang.net;
        vajram fetcher(string requestUrl) out string~ permit callers public {
          string body = callHttp(url = requestUrl);
          out ~ { body }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    String fetcher = Files.readString(outDir.resolve("system/fetcher.rs"));
    assertThat(fetcher)
        .contains("crate::vajram_rt::fetch_text(inputs.requestUrl.as_str())")
        .contains(".await");
  }

  @Test
  void rejectsReadFileAsStringForWasmWithoutEmittingTokioFilesystemIo(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("reader.vajram"),
        """
        package system;
        import vajram readFileAsString from lang.fileSystem;
        vajram reader(string filePath) out string {
          out readFileAsString(path = filePath);
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isFalse();
    assertThat(Files.exists(outDir.resolve("system/reader.rs"))).isFalse();
  }

  @Test
  void wasmTargetGeneratesAsyncDispatcherForAsyncGraphs(@TempDir Path tempDir) throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("dispatch.vajram"),
        """
        package external;
        vajram greet(string name) out string~ permit callers `outsideProcess public {
          ~ { name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir, RustCompilerMain.Target.WASM)).isTrue();
    assertThat(Files.readString(outDir.resolve("wasm_dispatch.rs")))
        .contains("pub async fn outside_process_greet(name: String) -> String")
        .contains("::call(")
        .contains(".await")
        .contains(".into_iter()");
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
        vajram firstLeaf() out string~ { ~ { "first" } }
        vajram secondLeaf() out string~ { ~ { "second" } }
        vajram parent() out string~ {
          string first = firstLeaf();
          string second = secondLeaf();
          string firstValue = first + "";
          string secondValue = second + "";
          out ~ { firstValue }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String parent = Files.readString(outDir.resolve("graph/graph.rs"));
    // `first`/`second` are independent, and each is wrapped as a `Shared` future so it can be
    // awaited by exactly the facet(s) that need it. `firstValue` only clones+awaits `first_fut`
    // (never `second_fut`), and `secondValue` only clones+awaits `second_fut` - so neither of
    // them can be blocked by the other's dependency, unlike a level-grouped join.
    assertThat(parent).contains("let first_fut = async move {");
    assertThat(parent).contains("let second_fut = async move {");
    assertThat(parent)
        .contains(
            "        let _firstValue_needs_first = first_fut.clone();\n"
                + "        let firstValue_fut = async move {\n"
                + "            let first = _firstValue_needs_first.await;\n"
                + "            &*arena.alloc(first + \"\")\n"
                + "        }")
        .contains(
            "        let _secondValue_needs_second = second_fut.clone();\n"
                + "        let secondValue_fut = async move {\n"
                + "            let second = _secondValue_needs_second.await;\n"
                + "            &*arena.alloc(second + \"\")\n"
                + "        }")
        .doesNotContain("_secondValue_needs_first")
        .doesNotContain("_firstValue_needs_second")
        .contains(
            "let (first, second, firstValue, secondValue) = futures::join!(first_fut.clone(),"
                + " second_fut.clone(), firstValue_fut.clone(), secondValue_fut.clone());");
    assertThat(parent.indexOf("let second_fut ="))
        .isLessThan(parent.indexOf("let firstValue_fut ="));
  }

  @Test
  void eachFacetAwaitsOnlyItsOwnDependenciesNotUnrelatedSiblings(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("diamond.vajram"),
        """
        package diamond;
        vajram leafA() out string~ { ~ { "a" } }
        vajram leafB() out string~ { ~ { "b" } }
        vajram leafC() out string~ { ~ { "c" } }
        vajram diamond() out string~ {
          string a = leafA();
          string b = leafB();
          string c = leafC();
          string combined = a + b;
          out ~ { combined }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String diamond = Files.readString(outDir.resolve("diamond/diamond.rs"));
    // `combined` depends on both `a` and `b`, but not on the unrelated independent `c` - it must
    // clone+await exactly `a_fut` and `b_fut`, and never reference `c_fut` at all. This is the
    // case the old level-grouped `join!` got wrong: `c` would have shared a level with `a`/`b`
    // and `combined` would have been forced to wait on it regardless.
    assertThat(diamond).contains("let a_fut = async move {");
    assertThat(diamond).contains("let b_fut = async move {");
    assertThat(diamond).contains("let c_fut = async move {");
    assertThat(diamond)
        .contains("let _combined_needs_a = a_fut.clone();")
        .contains("let _combined_needs_b = b_fut.clone();")
        .contains(
            "let combined_fut = async move {\n"
                + "            let a = _combined_needs_a.await;\n"
                + "            let b = _combined_needs_b.await;\n"
                + "            &*arena.alloc(a + b)\n"
                + "        }");
    String combinedBody =
        diamond.substring(
            diamond.indexOf("let combined_fut ="), diamond.indexOf("let (a, b, c, combined)"));
    assertThat(combinedBody).doesNotContain("c_fut");
    assertThat(diamond)
        .contains(
            "let (a, b, c, combined) = futures::join!(a_fut.clone(), b_fut.clone(),"
                + " c_fut.clone(), combined_fut.clone());");
  }

  @Test
  void lowersStringConcatenationWithStringInputsToRustStringSlices(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.vajram"),
        """
        package hello;
        vajram greet(string name) out string {
          { "Hello, " + name }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("hello/hello.rs")))
        .contains("\"Hello, \".to_string() + inputs.name.as_str()");
  }

  @Test
  void lowersChainedStringConcatenationWithTrailingStringLiterals(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("hello.vajram"),
        """
        package hello;
        vajram greet(string name) out string {
          { "Hello, " + name + "!" }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    assertThat(Files.readString(outDir.resolve("hello/hello.rs")))
        .contains("\"Hello, \".to_string() + inputs.name.as_str() + \"!\"");
  }

  @Test
  void lowersNumericPrimitiveAdditionWithDereferencedBoundaryValues(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("math.vajram"),
        """
        package math;
        vajram sumInts(int left, int right) out int { { left + right } }
        vajram sumFloats(float left, float right) out float { { left + right } }
        vajram sumDoubles(double left, double right) out double { { left + right } }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("math/math.rs"));
    assertThat(generated).contains("arena.alloc(*inputs.left + *inputs.right)");
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
        import vajram concatStrings from lang.strings;
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
        .contains("crate::combined::combined::leaf::call(")
        .contains("crate::combined::combined::leaf::LeafInputs {}")
        .contains("single-item Vajram batch");
  }

  @Test
  void batchesFanoutDependencyInputsIntoOneCalleeInvocation(@TempDir Path tempDir)
      throws IOException {
    Path sourceDir = tempDir.resolve("vajram");
    Path outDir = tempDir.resolve("out");
    Files.createDirectories(sourceDir);
    Files.writeString(
        sourceDir.resolve("fanout.vajram"),
        """
        package fanout;
        vajram leaf(string value) out string { { value } }
        vajram parent() out List<string> {
          string... values = ["one", "two"];
          string... results = leaf(value =... values);
          { results }
        }
        """);

    assertThat(RustCompilerMain.compile(sourceDir, outDir)).isTrue();
    String generated = Files.readString(outDir.resolve("fanout/fanout.rs"));
    assertThat(generated)
        .contains("leaf::call(")
        .contains("values")
        .contains(".into_iter()")
        .contains(".map(|it|")
        .contains("LeafInputs {")
        .contains("value: arena.alloc(it)")
        .doesNotContain("futures::future::join_all");
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
